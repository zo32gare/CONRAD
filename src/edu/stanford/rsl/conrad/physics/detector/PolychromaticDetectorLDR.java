package edu.stanford.rsl.conrad.physics.detector;

import java.io.IOException;
import java.util.ArrayList;

import edu.stanford.rsl.conrad.data.numeric.Grid2D;
import edu.stanford.rsl.conrad.physics.PhysicalObject;
import edu.stanford.rsl.conrad.physics.PolychromaticXRaySpectrum;
import edu.stanford.rsl.conrad.physics.absorption.AbsorptionModel;
import edu.stanford.rsl.conrad.physics.absorption.PolychromaticAbsorptionModel;
import edu.stanford.rsl.conrad.utils.CONRAD;
import edu.stanford.rsl.conrad.utils.UserUtil;

public class PolychromaticDetectorLDR extends XRayDetector{

	public enum ACQUISITION_TYPE
	{
		LOWDOSE, OVEREXPOSURE, NONE
	}	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6754471262497196107L;

	boolean configured = false;
	//PolychromaticAbsorptionModel model;
	boolean photonCounting = false;
	int dynamicRange = 14;
	ACQUISITION_TYPE type;
	double IMax = CONRAD.BIG_VALUE;

	@Override
	public void configure(){
		try {
			//photonCounting = UserUtil.queryBoolean("Do you want to simulate a photon counting detector?");
			ArrayList<Object> modelList = CONRAD.getInstancesFromConrad(PolychromaticAbsorptionModel.class);
			Object [] modelArray = new PolychromaticAbsorptionModel [modelList.size()];
			modelList.toArray(modelArray);
			model = (PolychromaticAbsorptionModel) UserUtil.chooseObject("Select noise-free model", "Model Selection", modelArray, (Object) modelArray[0]);
			model.configure();
			configured = true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void configure(AbsorptionModel absorpMod) throws Exception {
		try {
			photonCounting = UserUtil.queryBoolean("Do you want to simulate a photon counting detector?");
			model = absorpMod;
			model.configure();
			configured = true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void configure(AbsorptionModel absorpMod, boolean photonCounting) {
		this.photonCounting = photonCounting;
		try {
			model = absorpMod;
			model.configure();
			configured = true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void configure(AbsorptionModel absorpMod, boolean photonCounting, int dynamicRange) {
		this.photonCounting = photonCounting;
		this.dynamicRange = dynamicRange;
		try {
			model = absorpMod;
			model.configure();
			configured = true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void configure(boolean photonCounting, int dynamicRange, PolychromaticXRaySpectrum spectrum, ACQUISITION_TYPE type) {
		this.photonCounting = photonCounting;
		this.dynamicRange = dynamicRange;
		this.type = type;
		try {
			model = new PolychromaticAbsorptionModel();
			model.configure();
			((PolychromaticAbsorptionModel) model).setInputSpectrum(spectrum);
			configured = true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString(){
		String name = "Polychromatic X-Ray Detector LDR";
		if (model!= null) name+=" " + model.toString();
		return name;
	}

	@Override
	public boolean isConfigured(){
		return configured;
	}

	@Override	
	public void writeToDetector(Grid2D grid, int x, int y, ArrayList<PhysicalObject> segments){
		double value = ((PolychromaticAbsorptionModel) model).computeIntensity(segments, ((PolychromaticAbsorptionModel) model).getMinimalEnergy(), ((PolychromaticAbsorptionModel) model).getMaximalEnergy(), false, false);
		double I0 = ((PolychromaticAbsorptionModel) model).getTotalPhotonFlux();
//		double I0 = ((PolychromaticAbsorptionModel) model).getTotalIntensity();
		
		if (x == 0 && y == 0) System.out.println("I0 " + type.toString() + ": " + I0);
		//if (x == 0 && y == 0) System.out.println("nrBins = " + Math.pow(2, dynamicRange) + ", binWidth = " + (IMax / Math.pow(2, dynamicRange)));
		
		switch(type) {
		case LOWDOSE:
			value = bin(value, dynamicRange, IMax);
			value /= I0;
			break;
		case OVEREXPOSURE:
			if (value > IMax) {
				value = IMax;
			}
			value = bin(value, dynamicRange, IMax);
			value /= I0;
			break;
		case NONE:
			value = bin(value, dynamicRange, IMax);
			value /= I0;
			break;
		default:
			break;
		};
			


		if(value > 1){
			value = 1;
			//throw new RuntimeException("PolychromaticAbsorptionModel: numerical instability found.");
		}
		//End
		grid.putPixelValue(x, y,  -Math.log(value));
//		grid.putPixelValue(x, y, value);
		
	}



	private double bin(double value, int bitDepth, double maximum) {
		
		double nrBins = Math.pow(2, bitDepth);
		double binWidth = maximum / nrBins;
		
//		double binWidth = 34.5684194339045;
//		double nrBins = maximum / binWidth; 
		
//		System.out.println("nrBins = " + nrBins);
//		System.out.println("binWidth = " + binWidth);
		
		double binNr = 0;
		if (value % binWidth == 0) {
			binNr = Math.floor((value * nrBins)/maximum);
		} else {
			binNr = Math.floor((value * nrBins)/maximum) + 1;
		}
		
		// ?? overexposure
		// if (binNr >= nrBins) binNr = nrBins-1;
		
		return binNr* binWidth;
	
	}

	/**
	 * @return the photonCounting
	 */
	public boolean isPhotonCounting() {
		return photonCounting;
	}

	/**
	 * @param photonCounting the photonCounting to set
	 */
	public void setPhotonCounting(boolean photonCounting) {
		this.photonCounting = photonCounting;
	}

	/**
	 * @param configured the configured to set
	 */
	public void setConfigured(boolean configured) {
		this.configured = configured;
	}
	
	public double getIMax() {
		return IMax;
	}

	public void setIMax(double iMax) {
		IMax = iMax;
	}
	
}
