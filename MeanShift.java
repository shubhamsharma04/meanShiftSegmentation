import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * @author Shubham
 *
 */
public class MeanShift {
	static int h = 70;
	static int iter = 35;
	static int hs = 85;
	static int hr = 200;
	static double c = 1d;
	static double hsTable[];
	static double hrTable[] = new double[256];
	static int regionThreshold = 1000;

	public static void main(String[] args) throws IOException {
		// long time = System.currentTimeMillis();
		String filePath = "Image_Butterfly.jpg";
		String segmentedImage = "Image_Butterfly_segmented_" + h + "_" + iter + "_" + hs + "_" + hr + ".jpg";
		String contouredImage = "Image_Butterfly_contoured_" + h + "_" + iter + "_" + hs + "_" + hr + ".jpg";
		BufferedImage image;
		int width;
		int height;
		File input = new File(filePath);
		image = ImageIO.read(input);
		width = image.getWidth();
		height = image.getHeight();
		int biggerDim = width > height ? width : height;
		hsTable = new double[biggerDim + 1];
		int count = 0;
		int[][] meanShiftVectorArr = new int[width * height][7];
		// Read a colored image in java. Credit :
		// https://www.tutorialspoint.com/java_dip/understand_image_pixels.htm
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				Color c = new Color(image.getRGB(j, i));
				meanShiftVectorArr[count][0] = c.getRed();
				meanShiftVectorArr[count][1] = c.getGreen();
				meanShiftVectorArr[count][2] = c.getBlue();
				meanShiftVectorArr[count][3] = j;
				meanShiftVectorArr[count++][4] = i;
			}
		}
		performMeanShiftSegmentation(meanShiftVectorArr, width, height);
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		int currCount = 0;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int p = (255 << 24) | (meanShiftVectorArr[currCount][0] << 16) | (meanShiftVectorArr[currCount][1] << 8)
						| meanShiftVectorArr[currCount][2];
				bufferedImage.setRGB(j, i, p);
				currCount++;
			}
		}
		// Write a color Image in java. Credit :
		// https://www.dyclassroom.com/image-processing-project/how-to-get-and-set-pixel-value-in-java
		ImageIO.write(bufferedImage, "jpg", new File(segmentedImage));
		BufferedImage countouredImage = performBoundryTracingViaEightNghbr(image, meanShiftVectorArr);
		ImageIO.write(countouredImage, "jpg", new File(contouredImage));
		// System.out.println("Time taken : "+(System.currentTimeMillis() -
		// time));
	}

	private static BufferedImage performBoundryTracingViaEightNghbr(BufferedImage image, int[][] meanShiftVectorArr) {
		int width = image.getWidth();
		int height = image.getHeight();
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int currX = 0;
		int currY = 0;
		resetMeanShiftVectorArr(meanShiftVectorArr);
		boolean doContinue = true;
		while (doContinue) {
			doContinue = false;
			// Find starting point for region
			for (int i = currX; i < height; i++) {
				for (int j = currY; j < width; j++) {
					// Set a pixel in java Credit :
					// https://www.dyclassroom.com/image-processing-project/how-to-get-and-set-pixel-value-in-java
					if (meanShiftVectorArr[i * width + j][5] > 0) {
						boolean isBoundry = isFourNeighbourBoun(meanShiftVectorArr, i, j, width, height);
						if (isBoundry) {
							int p = (255 << 24) | (255 << 16) | (255 << 8) | 255;
							bufferedImage.setRGB(j, i, p);
						} else {
							int p = (255 << 24) | (meanShiftVectorArr[i * width + j][0] << 16)
									| (meanShiftVectorArr[i * width + j][1] << 8)
									| meanShiftVectorArr[i * width + j][2];
							bufferedImage.setRGB(j, i, p);
						}
					} else {
						int p = (255 << 24) | (meanShiftVectorArr[i * width + j][0] << 16)
								| (meanShiftVectorArr[i * width + j][1] << 8) | meanShiftVectorArr[i * width + j][2];
						bufferedImage.setRGB(j, i, p);
					}
				}
			}
		}
		return bufferedImage;
	}

	private static boolean isFourNeighbourBoun(int[][] meanShiftVectorArr, int i, int j, int width, int height) {
		boolean isboundary = false;
		int curentRegion = meanShiftVectorArr[i * width + j][5];
		if (i == 0 || j == 0 || i == height - 1 || j == width - 1) {
			isboundary = true;
		} else if (curentRegion != meanShiftVectorArr[((i - 1) * width) + j][5]) {
			isboundary = true;
		} else if (curentRegion != meanShiftVectorArr[((i) * width) + j - 1][5]) {
			isboundary = true;
		} else if (curentRegion != meanShiftVectorArr[((i) * width) + j + 1][5]) {
			isboundary = true;
		} else if (curentRegion != meanShiftVectorArr[((i + 1) * width) + j][5]) {
			isboundary = true;
		}
		return isboundary;
	}

	private static void resetMeanShiftVectorArr(int[][] meanShiftVectorArr) {
		int meanShiftVectorArrSize = meanShiftVectorArr.length;
		for (int i = 0; i < meanShiftVectorArrSize; i++) {
			meanShiftVectorArr[i][6] = 0;
		}
	}

	private static void performMeanShiftSegmentation(int[][] meanShiftVectorArr, int width, int height) {
		boolean doContinue = true;
		int meanShiftVectorArrSize = width * height;
		int region = 1;
		populateLookUpTables();
		while (doContinue) {
			doContinue = false;
			int currMean = 0;
			for (int i = 0; i < meanShiftVectorArrSize; i++) {
				// if current pixel is unlabled and hasn't already been
				// considered as mean in this iteration
				if (meanShiftVectorArr[i][5] == 0 && meanShiftVectorArr[i][6] == 0) {
					doContinue = true;
					meanShiftVectorArr[i][6] = 1;
					currMean = i;
					break;
				}
			}
			if (doContinue) {
				List<Integer> similarPixels = new ArrayList<Integer>();
				for (int i = 0; i < meanShiftVectorArrSize; i++) {
					if (i != currMean && meanShiftVectorArr[i][5] == 0) {
						int eucDist = (int) Math.sqrt(Math
								.pow(meanShiftVectorArr[currMean][0] - meanShiftVectorArr[i][0]
										* getINorm(meanShiftVectorArr[currMean][0], meanShiftVectorArr[i][0]), 2)
								+ Math.pow(
										meanShiftVectorArr[currMean][1] - meanShiftVectorArr[i][1]
												* getINorm(meanShiftVectorArr[currMean][1], meanShiftVectorArr[i][1]),
										2)
								+ Math.pow(
										meanShiftVectorArr[currMean][2] - meanShiftVectorArr[i][2]
												* getINorm(meanShiftVectorArr[currMean][2], meanShiftVectorArr[i][2]),
										2)
								+ Math.pow(
										meanShiftVectorArr[currMean][3] - meanShiftVectorArr[i][3]
												* getXNorm(meanShiftVectorArr[currMean][3], meanShiftVectorArr[i][3]),
										2)
								+ Math.pow(
										meanShiftVectorArr[currMean][4] - meanShiftVectorArr[i][4]
												* getXNorm(meanShiftVectorArr[currMean][3], meanShiftVectorArr[i][3]),
										2));
						if (eucDist <= h) {
							similarPixels.add(i);
						}
					}
				}
				int sizeOfRegion = similarPixels.size();
				int newMean[] = new int[5];
				newMean[0] += meanShiftVectorArr[currMean][0];
				newMean[1] += meanShiftVectorArr[currMean][1];
				newMean[2] += meanShiftVectorArr[currMean][2];
				newMean[3] += meanShiftVectorArr[currMean][3];
				newMean[4] += meanShiftVectorArr[currMean][4];
				for (int i = 0; i < sizeOfRegion; i++) {
					newMean[0] += meanShiftVectorArr[similarPixels.get(i)][0];
					newMean[1] += meanShiftVectorArr[similarPixels.get(i)][1];
					newMean[2] += meanShiftVectorArr[similarPixels.get(i)][2];
					newMean[3] += meanShiftVectorArr[similarPixels.get(i)][3];
					newMean[4] += meanShiftVectorArr[similarPixels.get(i)][4];
				}
				sizeOfRegion++;
				newMean[0] /= sizeOfRegion;
				newMean[1] /= sizeOfRegion;
				newMean[2] /= sizeOfRegion;
				newMean[3] /= sizeOfRegion;
				newMean[4] /= sizeOfRegion;
				int meanShiftValue = (int) Math.sqrt(Math.pow(meanShiftVectorArr[currMean][0] - newMean[0], 2)
						+ Math.pow(meanShiftVectorArr[currMean][1] - newMean[1], 2)
						+ Math.pow(meanShiftVectorArr[currMean][2] - newMean[2], 2)
						+ Math.pow(meanShiftVectorArr[currMean][3] - newMean[3], 2)
						+ Math.pow(meanShiftVectorArr[currMean][4] - newMean[4], 2));
				if (meanShiftValue < iter) {
					// Hoooorrray
					sizeOfRegion--;

					int regionLabel = sizeOfRegion > regionThreshold ? region : -1;
					for (int i = 0; i < sizeOfRegion; i++) {
						meanShiftVectorArr[similarPixels.get(i)][0] = newMean[0];
						meanShiftVectorArr[similarPixels.get(i)][1] = newMean[1];
						meanShiftVectorArr[similarPixels.get(i)][2] = newMean[2];
						meanShiftVectorArr[similarPixels.get(i)][5] = regionLabel;
					}
					region++;
				}
			}
		}
		// System.out.println("Total number of regions found : "+region);
	}

	private static double getXNorm(int currMen, int currElem) {
		double result = 0d;
		int diff = Math.abs(currMen - currElem);
		if (diff < hs) {
			result = hsTable[diff];
		}
		return result;
	}

	private static double getINorm(int currMen, int currElem) {
		double result = 0d;
		int diff = Math.abs(currMen - currElem);
		if (diff < hr) {
			result = hrTable[diff];
		}
		return result;
	}

	private static void populateLookUpTables() {
		double hsNorm = hs * hs;
		double hrNorm = hr * hr;
		hsTable[0] = 1;
		hrTable[0] = 1;
		// hs lookup table
		for (int i = 1; i <= hs; i++) {
			double xNorm = (double) i / hsNorm;
			hsTable[i] = c * (1.0d - Math.pow(xNorm, 2));
		}
		// hr lookup table
		for (int i = 1; i < hr; i++) {
			double iNorm = (double) i / hrNorm;
			hrTable[i] = c * (1.0d - Math.pow(iNorm, 2));
		}
	}

}
