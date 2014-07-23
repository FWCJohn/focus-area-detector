package andrewy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
* MapReduce Focus Area Detector is a cloud-based program capable of distinguishing the difference between a sharp line and a blurred line, and can highlight in-focus areas of the input images.
*
* @author  Chao-te Andrew Yang
* @version 1.0
* @since   2013-8-5 
*/
public class ConvertImage {

	// this is non cloud-based version of the detector
	public static void main(String[] args) throws IOException {
		BufferedImage input = null;
		String fileName = "";
		Color firstColor, nextColor, anotherColor, newColor;
		int firstAvgColor = 0, nextAvgColor = 0, anotherAvgColor = 0, score = 0, d = 0; 
		
		if ((args.length) != 1) {
			System.err.println("Usage: ConvertImage [inputPath]");
			return;
		} else {
			// Read the image file
			input = ImageIO.read(new File(args[0]));
			
			// Save the file name without extension
			fileName = new File(args[0]).getName();
		}

		System.out.println("\nImage size: " + input.getWidth() + "x" + input.getHeight());
		System.out.print("Processing " + fileName + "......");
		
		// Loop through every pixel in the image
		for (int w = 0; w < input.getWidth(); w++) {
			for (int h = 0; h < input.getHeight(); h++) {
				
				// Save the RGB values of the pixel at (w,h)
				firstColor = new Color(input.getRGB(w, h));
				// Save the RGB values of the pixel at (w + 1, h)
				nextColor = new Color(input.getRGB((w < (input.getWidth()-1) ? w+1 : w), h));
				// Save the RGB values of the pixel at (w, h + 1)
				anotherColor = new Color(input.getRGB(w, (h < (input.getHeight()-1) ? h+1 : h)));
				
				// Get the average RGB value of these pixels
				firstAvgColor = ((firstColor.getRed() + firstColor.getGreen() + firstColor.getBlue()) / 3);
				nextAvgColor = ((nextColor.getRed() + nextColor.getGreen() + nextColor.getBlue()) / 3);
				anotherAvgColor = ((anotherColor.getRed() + anotherColor.getGreen() + anotherColor.getBlue()) / 3);
				
				d = ( (Math.abs(firstAvgColor - nextAvgColor)) + (Math.abs(firstAvgColor - anotherAvgColor)) ) / 2;
				score += d;
				
				if( d <= 10 )
					newColor = new Color(firstAvgColor, firstAvgColor, firstAvgColor);
				else	
					newColor = new Color(255, 0, 0);
					
				// Reset the RGB value for the pixel at (w,h)
				input.setRGB(w, h, newColor.getRGB());
			}
		}

		System.out.print("completed");
		System.out.println("\nTotal score: " + score + "\n");
		
		File file = new File("cs755-project-sample//output//" + fileName.replaceFirst("[.][^.]+$", "") + "-out.jpg");
		file.getParentFile().mkdirs();
		ImageIO.write(input, "jpg", file);
	}
}