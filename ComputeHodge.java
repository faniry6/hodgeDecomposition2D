
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
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
import jv.loader.PgLoader;
import jv.object.PsDebug;

/**
 * Compute Hodge decomposition of given geometry defined in a folder
 * @author Faniry Razafindrazaka, created (21.03.2019)
 *
 */
public class ComputeHodge {
	protected static String PATH_FOLDER="D:\\faniry\\Project\\ECMath\\model\\Converter\\";
	protected static String ERROR = "ERROR::";
	protected static String WARNING = "WARNING::";
	// Limit number of loaded geometry to avoid Java heap space
	protected static int 	MAXIMUM_NO_GEOM = 20;
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
		
		// Load geometry
		File folder = new File(PATH_FOLDER);
		final String[] listOfStls = folder.list(new FilenameFilter(){
			public boolean accept(File directory, String fileName){
				return fileName.endsWith(".jvx");
			}
		});
		
		if(listOfStls.length == 0){
			System.out.println(ERROR+"missing geometry, abort");
		}
		// This could be modified
		if(listOfStls.length > MAXIMUM_NO_GEOM){
			System.out.println("WARNING :: for memory optimization only the first "+MAXIMUM_NO_GEOM+" will be processed");
			MAXIMUM_NO_GEOM = 20;
		}else
			MAXIMUM_NO_GEOM = listOfStls.length;
		
		// Load geometry file
		System.out.println("Loading geometries...");
		final PgElementSet[] geom = new PgElementSet[MAXIMUM_NO_GEOM];
		PgLoader loader = new PgLoader();
		for(int i=0;i<MAXIMUM_NO_GEOM;i++){
			geom[i] = (PgElementSet)loader.loadGeometry(PATH_FOLDER+listOfStls[i])[0];
		}
		// Compute Hodge decomposition
		if(m_threadPoolShutDown) {
			m_threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-1);
			m_pool = new ExecutorCompletionService<Boolean>(m_threadPool);
		}
		int incr = 0;
		Future<Boolean>[] task = new Future[MAXIMUM_NO_GEOM];
		for(int i=0;i<MAXIMUM_NO_GEOM;i++){
			final int ii=i;
			task[ii] = m_pool.submit(new Callable<Boolean>(){
				@Override
				public Boolean call(){
					PwHMF hodge = new PwHMF();
					hodge.setGeometry(geom[ii]);
					hodge.setDecompositionMode(PwHMF.DECOMPOSITION_MODE_FIVETERM);
					hodge.decompose();
					return true;
				}
			});
		}
		System.out.println("Computing Hodge...");
		for(Future<Boolean> f:task){
			try {
				if(f.get().booleanValue()){
					System.out.println(geom[incr].getName()+" is processed");
					incr++;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Save CSV
		System.out.println("CSV file saved at "+PATH_FOLDER);
		FileWriter writer = new FileWriter(PATH_FOLDER+"hodge_statistics.csv");
		writer.write("sep=,");
		writer.write("\n");
		writer.write(",Exact,Coexact,HCenter,HNemann,HDirichlet");
		writer.write("\n");
		double norm = -1;
		double inputNorm = -1;
		incr = 0;
		for(int i=0;i<MAXIMUM_NO_GEOM;i++){
			writer.write(geom[i].getName());
			inputNorm = PuHMFUtil.norm(geom[i].getVectorField(0), PuHMFUtil.NORM_L2);
			// Add and compute vector field norm
			for(int j=0;j<5;j++){				
				norm = PuHMFUtil.norm(geom[i].getVectorField(j+1),  PuHMFUtil.NORM_L2);
				norm = norm*norm/(inputNorm*inputNorm);
				if(norm < 1e-10)
					norm = 0.;
				writer.write(","+norm);
			}
			writer.write("\n");
			incr ++;
		}
		writer.close();
		
		System.out.println("Finish");

	}

}
