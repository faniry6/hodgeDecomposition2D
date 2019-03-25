
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dev6.vector.PuHMFUtil;
import dev6.vector.PwHMF;

import jv.geom.PgElementSet;
import jv.geom.PgPointSet;
import jv.geom.PgVectorField;
import jv.loader.PgLoader;
import jv.object.PsDebug;
import jv.object.PsUtil;
import jv.project.PgGeometry;
import jv.vecmath.PdBary;
import jv.vecmath.PdVector;
import jv.vecmath.PiVector;

import jvx.geom.PwBary;
import jvx.geom.PwModel;


/**
 * Convert a starccm+ data to jvx data
 * @author Faniry Razafindrazaka
 * @created 23.03.2019
 */
public class Converter_starccm_to_jvx_gz {
	// Folder containing the files
	protected static String PATH_FOLDER;
	// Limit number of loaded geometry to avoid Java heap space
	protected static int 	MAXIMUM_NO_GEOM = 20;
	// Boolean to check if interpolation is needed
	private static boolean m_needInterpolation = false;
	// Temporary clone geometry to check for compatibility
	private static PgElementSet m_dual;
	// For multithreading 
	protected 		static ExecutorService 			m_threadPool;
	protected 		static CompletionService<Boolean> 	m_pool;
	protected		static boolean			            m_threadPoolShutDown = true;
	
	public static void main(String[] args) throws IOException {
		if(args.length == 0){
			System.out.println("Missing argument, path to folder");
		}else if(args.length == 1){// except the folder path
			PATH_FOLDER = args[0];
		}else if(args.length == 2){
			MAXIMUM_NO_GEOM = Integer.parseInt(args[0]);
			PATH_FOLDER = args[1];
			System.out.println("Maximum number of allowed geometry is set to "+MAXIMUM_NO_GEOM);
		}
		
		if(PATH_FOLDER.charAt(PATH_FOLDER.length()-1)!='\\'){
			PATH_FOLDER +="\\";
		}
		
		System.out.println("Folder path "+PATH_FOLDER);
		// Load and print all files with *.stl ending in the folder
		File folder = new File(PATH_FOLDER);
		final String[] listOfStls = folder.list(new FilenameFilter(){
			public boolean accept(File directory, String fileName){
				return fileName.endsWith(".stl");
			}
		});
		// This could be modified
		if(listOfStls.length > MAXIMUM_NO_GEOM){
			System.out.println("WARNING :: for memory optimization only the first "+MAXIMUM_NO_GEOM+" will be processed");
			MAXIMUM_NO_GEOM = 20;
		}else
			MAXIMUM_NO_GEOM = listOfStls.length;
		
		// Load geometry file
		System.out.println("Loading geometries...");
		final PgElementSet[] geom = new PgElementSet[MAXIMUM_NO_GEOM];
		loadGeomtryFile(geom,listOfStls);
		
		final String[] listOfCsv = folder.list(new FilenameFilter(){
			public boolean accept(File directory, String fileName){
				return fileName.endsWith(".csv");
			}
		});
		
		// Load vector file
		System.out.println("Loading vectors...");
		final PgPointSet[] vec_geom = new PgPointSet[MAXIMUM_NO_GEOM];
		loadVectorFile(vec_geom,listOfCsv);
		
		System.out.println("Compute mapping...");
		
		if(m_threadPoolShutDown) {
			m_threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-1);
			m_pool = new ExecutorCompletionService<Boolean>(m_threadPool);
		}
		Future<Boolean>[] task = new Future[MAXIMUM_NO_GEOM];
		
		for(int i=0;i<MAXIMUM_NO_GEOM;i++){
			final int ii=i;
			task[ii] = m_pool.submit(new Callable<Boolean>(){
				@Override
				public Boolean call(){
					// compute mapping
					PiVector notMapped = mapVectorFieldUsingCentroid(vec_geom[ii], geom[ii], true);
					System.out.println("SAVING(JVX)::Geometry "+ii);
					(new PgLoader()).saveGeometry(new PgGeometry[]{(PgGeometry)geom[ii]}, PATH_FOLDER+PsUtil.getFileBaseName(listOfStls[ii])+".jvx");
					if(notMapped.getSize() != 0){ 
						System.out.println("WARNING::Geometry "+ii+" has zero vector" );
					}else
						System.out.println("SUCCESS::Geometry "+ii);
					return true;
				}
			});
		}
		int incr = 0;
		for(Future<Boolean> f:task){
			try {
				if(f.get().booleanValue()){
					System.out.println(geom[incr].getName()+" is processed");
					incr++;
					if(incr == MAXIMUM_NO_GEOM){
						System.out.println("Finish!");
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		m_threadPoolShutDown = true;
		m_threadPool.shutdown();
	}
	/**
	 * This method assumes that the position is exactly mapped to the centroid of 
	 * each face.
	 * @param geom TODO
	 * @param showStatus TODO
	 */
	public static PiVector mapVectorFieldUsingCentroid(PgPointSet vector, PgElementSet geom, boolean showStatus){
		int numElements = geom.getNumElements();
		int numVec = vector.getNumVertices();
		
		PdVector[] centroid = new PdVector[numElements];
		PdVector.realloc(centroid, numElements, 3);
		for(int i=0;i<numElements;i++){
			PdVector[] vertex = geom.getElementVertices(i);
			for(PdVector v:vertex)
				centroid[i].add(v);
			centroid[i].multScalar(1./3);
		}
		
		PdVector[] vectorPerElement = new PdVector[numElements];
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		
		int lastCenter = -1;
		
		geom.makeNeighbour();
		
		int indv = 0;
		Queue<Integer> queue = new LinkedList<Integer>();
		boolean[] visited = new boolean[geom.getNumElements()];
		boolean found = false;
		int iter = 0;
		for(int i=0;i<numVec;i++){
			if(showStatus){
				if(i== numVec/10*iter){
					System.out.print(".");
					iter++;
				}
			}
			if(lastCenter == -1){
				for(int j=0;j<numElements;j++){// Go linear
					if(vectorPerElement[j]!=null)
						continue;
					if(PdVector.dist(centroid[j], vector.getVertex(i))<1e-8){// This might be not accurate 
						vectorPerElement[j] = vector.getVectorField(0).getVector(i);
						lastCenter = j;
						break;
					}
				}
			}else{
				// Grow disc from last center, assume that vertex are close to each other
				queue.clear();
				for(int elt=0;elt<geom.getNumElements();elt++){
					visited[elt] = false;
				}
				queue.add(lastCenter);
				found = false;
				indv = 0;
				while(!queue.isEmpty() && !found){
					int curr = queue.poll();
					if(PdVector.dist(centroid[curr], vector.getVertex(i))<1e-8){
						vectorPerElement[curr] = vector.getVectorField(0).getVector(i);
						lastCenter = curr;
						found = true;
					}else{
						for(int j=0;j<3;j++){
							int nb = geom.getNeighbour(curr).m_data[j];
							if(nb!=-1 && !visited[nb]){						
								queue.add(nb);
								visited[nb] = true;
							}
						}
						indv++;
					}
				}
				if(indv == geom.getNumElements()){
					// Accuracy cannot identify use projection
					System.out.println("Geometry "+geom.getName()+" : "+" position "+i+" trigger projection");
					PdBary outBary = new PdBary(3);
					PwBary.projectOntoElementSet(geom, vector.getVertex(i), outBary);
					if(outBary.m_elementInd == -1){
						System.out.println("Geometry "+geom.getName()+" : "+"did not succeed projection, abort!");
						break;
					}
					System.out.println("Geometry "+geom.getName()+" : "+"Successful projection!");
					if(vectorPerElement[outBary.m_elementInd]==null)
						vectorPerElement[outBary.m_elementInd] = vector.getVectorField(0).getVector(i);
					break;
				}
			}
		}
		 System.out.println();
		boolean foundNoVector = false;
		
		PiVector notMappedElt = new PiVector();
		for(int i=0;i<geom.getNumElements();i++){
			if(vectorPerElement[i]==null){
				notMappedElt.addEntry(i);
				vectorPerElement[i] = new PdVector(3);
				//System.out.print(i+" ");
			}
		}
		
		int nov = geom.getNumVertices();
		
		geom.removeAllVectorFields();
		
		PgVectorField v = new PgVectorField(3);
		v.setBasedOn(PgVectorField.ELEMENT_BASED);
		v.setGeometry(geom);
		geom.addVectorField(v);
		v = geom.getVectorField(0);
		v.setVectors(vectorPerElement);
		
		return notMappedElt;
	}
	/**
	 * @param geom
	 * @param vector
	 * @return true if compatible with the dual
	 */
	private static boolean compatible(PgElementSet geom, PgPointSet vector) {
//		System.out.println("DEBUG :: "+"Input points : "+vector.getNumVertices());
//		System.out.println("DEBUG :: "+"Num Triangle : "+geom.getNumElements());
		PgElementSet dual = (PgElementSet) geom.clone();
		PwModel.dual(dual);
//		System.out.println("DEBUG :: "+"Num point Dual : "+m_dual.getNumVertices());
		if(vector.getNumVertices() < dual.getNumVertices()){
			m_needInterpolation = true;
		}
		return true;
	}

	/**
	 * @param vector    
	 * @param listOfCsv
	 * @throws IOException 
	 */
	private static void loadVectorFile(PgPointSet[] vector, String[] listOfCsv) throws IOException {
		
		for(int strIdx=0;strIdx<MAXIMUM_NO_GEOM;strIdx++){
			BufferedReader in = PsUtil.open(PATH_FOLDER+listOfCsv[strIdx]);
			if (in == null) {
				if (PsDebug.WARNING) PsDebug.warning("could not open = " + listOfCsv[strIdx]);
				return;
			}
			String line = readLine(in);
			
//			while(line.contains("velocity_Magnitude"))// Assume that the file is not empty
//				line = readLine(in);
			
			line = readLine(in);
			ArrayList<PdVector> vectorFields = new ArrayList<PdVector>();
			ArrayList<PdVector> position	 = new ArrayList<PdVector>();
			
			while(line!=null) {
				String[] data = line.split(",");
				PdVector field = new PdVector(3);
				for(int i=1;i<=3;i++) {
					field.m_data[i-1] = Double.parseDouble(data[i]);
				}
				PdVector pos = new PdVector(3);
				for(int i=4;i<=6;i++) {
					pos.m_data[i-4] = Double.parseDouble(data[i]);
				}
				vectorFields.add(field);
				position.add(pos);
				line = readLine(in);
			}
			
			int numVector = vectorFields.size();
			
			PgVectorField vf = new PgVectorField(3);
			vf.setNumVectors(numVector);
			
			vector[strIdx] = new PgPointSet(3);
			vector[strIdx].setNumVertices(numVector);
			
			for(int i=0;i<numVector;i++){
				vector[strIdx].setVertex(i, position.get(i));
				vf.setVector(i, vectorFields.get(i));
			}
			vector[strIdx].addVectorField(vf);
		}
	}

	/**
	 * @param geom 
	 * @param listOfStls
	 */
	private static PgElementSet[] loadGeomtryFile(PgElementSet[] geom, String[] listOfStls) {
		PgLoader loader = new PgLoader();
		for(int i=0;i<MAXIMUM_NO_GEOM;i++){
			geom[i] = (PgElementSet)loader.loadGeometry(PATH_FOLDER+listOfStls[i])[0];
		}
		return geom;
	}
	
	private static String readLine(BufferedReader in) throws IOException {
		String line;
		do {
			line = in.readLine();
			if (line == null) return null;
			line = line.trim();
		} while ((line.length() == 0));
		return line;
	}
}
