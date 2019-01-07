import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tile Downloader Manager class.
 * 
 * @author aecavac
 *
 */
public class TileDownloaderManager {
	
	/**
	 * Configuration variables.
	 * 
	 * Ex. Tile services :
	 * http://b.tile.openstreetmap.org
	 * https://a.tiles.mapbox.com/v3/foursquare.meku766r
	 * 
	 * TODO : Add Url Pattern Support : {z}/{x}/{y}.png 
	 */
	
	private final static String tileService = "https://tiles.mapire.eu/mercator/cadastral";
	private final static String destPath = "/home/user/tiles_tmp/pag";
	private final static String headers[][] = { 
		{"Accept", "image/webp,image/apng,image/*,*/*;q=0.8"}, 
		{"Referer", "https://mapire.eu/en/map/cadastral/?bbox=1471886.7950656265%2C5456935.574662946%2C1877003.0449770605%2C5640384.44254737&map-list=1&layers=osm%2C3%2C4"} 
	};
	private final static int zoomStart = 9;
	private final static int zoomEnd = 18;
	
	/**
	 * Main method to run application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TileDownloaderManager tileDownloaderManager = new TileDownloaderManager();
		//tileDownloaderManager.downloadTilesSingleThread(tileService, destPath, zoomStart, zoomEnd);
		tileDownloaderManager.downloadTilesMultiThread(tileService, destPath, zoomStart, zoomEnd, headers);
	}
	
	/**
	 * Single Thread Tile Download
	 */	
	private void downloadTilesSingleThread(int zStart, int zEnd) {
		
		int zoomStart = zStart;
		int zoomEnd = zEnd;
		
		Date startDate = new Date();
		
		TileDownloader tileDownloader = new TileDownloader();
		
		tileDownloader.setTileService(tileService);
		tileDownloader.setDestPath(destPath);

		tileDownloader.setZoomLevelStartIndex(zoomStart);
		tileDownloader.setZoomLevelEndIndex(zoomEnd);

		tileDownloader.setxStartIndex(0);
		tileDownloader.setyStartIndex(0);

		tileDownloader.setxEndIndex(0);
		tileDownloader.setyEndIndex(0);

		Thread tileDownloaderThread = new Thread(tileDownloader);
		tileDownloaderThread.start();
		
		try {
			tileDownloaderThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Date endDate = new Date();

		long diffMill = endDate.getTime() - startDate.getTime();
		System.out.println("==============ZOOM " + zoomStart + "-" + zoomEnd + "==============");
		System.out.println("Time : " + diffMill);
		System.out.println("Img Count : " + TileDownloader.downImageCount);
		System.out.println("Avg : " + diffMill / TileDownloader.downImageCount);
		System.out.println("=====================================");
		
	}
	
	/**
	 * Multi Thread Tile Download
	 * 
	 * ZOOM : 0, 1, 2 	Thd : 1
	 * ZOOM : 3 		Thd : 3 ( 2^3/2 -1 )
	 * ZOOM : 4 		Thd : 7 ( 2^4/2 -1 )
	 * ZOOM : 5 		Thd : 15( 2^5/2 -1 )
	 * ZOOM : X >= 6	Thd : 32
	 */
	private void downloadTilesMultiThread(final String tileService, final String destPath, int zStart, int zEnd, final String headers[][]) {
		
		int zoomStart = zStart;
		int zoomEnd = zEnd;
		
		int numberOfThread = 0;
		Date startDate = new Date();

		// Divide works to threads
		List<Thread> threads = new ArrayList<>();

		for (int z = zoomStart; z <= zoomEnd; z++) {

			System.out.println("Zoom : " + z);
			
			//Calculate number of thread
			if(z < 3){
				numberOfThread = 1;
			}else if( z < 6 ){
				numberOfThread = (int)Math.pow(2, z-1) - 1;
			}else{
				numberOfThread = 32;
			}
			
			int totalXDirectoryNum = (int) (Math.pow(2, z));

			for (int xStart = 0; xStart < totalXDirectoryNum; ) {
				int xEnd;
				
				//If directory num enough for thread num.
				if(totalXDirectoryNum / numberOfThread > 1){
					xEnd = xStart + totalXDirectoryNum / numberOfThread - 1;
				}else{
					//If not then download all dirs.
					xEnd = 0;	
				}
				if(xEnd >= totalXDirectoryNum){
					xEnd = totalXDirectoryNum - 1;
				}

				TileDownloader tileDownloader = new TileDownloader();
				
				tileDownloader.setTileService(tileService);
				tileDownloader.setDestPath(destPath);

				tileDownloader.setZoomLevelStartIndex(z);
				tileDownloader.setZoomLevelEndIndex(z);

				tileDownloader.setxStartIndex(xStart);
				tileDownloader.setyStartIndex(0);

				tileDownloader.setxEndIndex(xEnd);
				tileDownloader.setyEndIndex(0);
				tileDownloader.setHeaders(headers);

				System.out.println("xStart:" + xStart + " xEnd:" + xEnd);
				Thread tileDownloaderThread = new Thread(tileDownloader);
				tileDownloaderThread.start();
				threads.add(tileDownloaderThread);
				
				if(totalXDirectoryNum / numberOfThread > 1){
					xStart += totalXDirectoryNum / numberOfThread;
				}else{
					//Don't continue..
					xStart = totalXDirectoryNum;	
				}
			}
			
			//Wait threads
			System.out.println("List size : " + threads.size());
			for (int i = 0; i < threads.size(); i++){
				try {
					((Thread)threads.get(i)).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		    	}
			
			System.out.println("---Clear Thread List---");
			threads.clear();
		}

		Date endDate = new Date();

		long diffMill = endDate.getTime() - startDate.getTime();
		System.out.println("==============ZOOM " + zoomStart + "-" + zoomEnd + "==============");
		System.out.println("Time : " + diffMill);
		System.out.println("Img Count : " + TileDownloader.downImageCount);
		System.out.println("Avg : " + diffMill / TileDownloader.downImageCount);
		System.out.println("====================================");
	}
}
