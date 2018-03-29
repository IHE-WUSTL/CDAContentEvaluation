package edu.wustl.mir.erl.IHETools.Util;

import edu.wustl.mir.erl.IHETools.CDAContentEvaluation.db.*;

public class ExporterThread implements Runnable {
	public void run() {
		System.out.println("Hello from a thread");
		
		boolean done = false;
		
		while (!done) {
			try {
				Exporter exporter = new Exporter();
				exporter.executeOnce();
				Thread.sleep(5000);
			} catch (Exception e) {
				e.printStackTrace();
				done = true;
			}
		}
	}
	
	
}
