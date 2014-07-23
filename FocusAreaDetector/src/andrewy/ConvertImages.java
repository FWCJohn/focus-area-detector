package andrewy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
* MapReduce Focus Area Detector is a cloud-based program capable of distinguishing the difference between a sharp line and a blurred line, and can highlight in-focus areas of the input images.
*
* @author  Chao-te Andrew Yang
* @version 1.0
* @since   2013-8-7 
*/
public class ConvertImages {
	
	// The Mapper class takes text input value, and maps pixel information
	public static class Map extends Mapper<LongWritable, Text, Text, Text>{
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			int score = 0, d = 0;
			int count = 0;
			String line = value.toString();
			String temp = "", fileName = "", w = "", h = "", firstAvgColor = "", nextAvgColor = "", anotherAvgColor = "";

			// Use the StringTokenizer to split each line in the input file
			StringTokenizer tokenizer = new StringTokenizer(line, "|");
			
			while(tokenizer.hasMoreElements()){
				// Get the next subString
				temp = tokenizer.nextElement().toString();
				
				// The pattern We are looking for is "fileName | firstAvgColor | nextAvgColor | anotherAvgColor | w | h "
				if(count == 0){
					//1st subString
					fileName = temp;
					count++;
				} else if(count == 1){
					//2nd subString
					firstAvgColor = temp;
					count++;
				} else if(count == 2){
					//3rd subString
					nextAvgColor = temp;
					count++;
				} else if(count == 3){
					//4th subString
					anotherAvgColor = temp;
					count++;
				} else if(count == 4){
					//5th subString
					w = temp;
					count++;
				} else if(count == 5){
					//6th subString
					h = temp;
					count = 0;
				}
			}
			
			// Calculate the average difference between (pixel-1, pixel-2) and (pixel-1, pixel-3)
			// The average RGB value for these 3 pixels has been transformed to grayscale by the PixelInfoBuilder class
			d = ( Math.abs((new Integer(firstAvgColor)) - (new Integer(nextAvgColor))) + 
					Math.abs((new Integer(firstAvgColor)) - (new Integer(anotherAvgColor))) ) / 2;
			score = d;

			// If the average difference is large enough, change the color of this pixel to red
			if (d <= 10)
				context.write(new Text(fileName), new Text(firstAvgColor + "|" + score + "|" + w + "|" + h));
			else if (d > 10)
				context.write(new Text(fileName), new Text("255|" + score + "|" + w + "|" + h));
		}
	}
	
	// The Reducer class takes the output from the Mapper class, resets the pixel color, and calculates the total score 
	public static class Reduce extends Reducer<Text, Text, Text, IntWritable>{
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			Color newColor = null;
			int count = 0, totalScore = 0, color = 0, w = 0, h = 0, temp = 0;
			String line = "";
			
			// Open the original photo as a BufferedImage, we'll create a new photo after the map reduce process
			BufferedImage img = ImageIO.read(new File("cs755-project-sample//" + key + ".jpg"));
			
			// The input values for reducer: "fileName, ["color|score|w|h", "color|score|w|h", "color|score|w|h" ... ]"
			StringTokenizer tokenizer;
			
			// Loop through list mapped by the Mapper class
			for(Text value : values){
				line = value.toString();
				
				// Split each line
				tokenizer = new StringTokenizer(line, "|");
				
				// Parse the subString to integer
				while(tokenizer.hasMoreElements()){
					temp = Integer.parseInt(tokenizer.nextElement().toString());
					
					if(count == 0){
						// 1st subString
						color = temp;
						
						if(color == 255)
							newColor = new Color(255, 0, 0);
						else {
							newColor = new Color(color, color, color);
						}
						
						count++;
					} else if(count == 1){
						// 2nd subString
						totalScore += temp;
						count++;
					} else if(count == 2){
						// 3rd subString
						w = temp;
						count++;
					} else if(count == 3){
						// 4th subString
						h = temp;
						count = 0;
					}
					
					//Reset RGB of the original photo (actually it's the BufferedImage object)
					img.setRGB(w, h, newColor.getRGB());
				}
			}
			
			// Create a new file
			File file = new File("cs755-project-sample//" + key.toString().replaceFirst("[.][^.]+$", "") + "-out.jpg");
			//file.getParentFile().mkdirs();
			
			// Write the pixel information to that new file
			ImageIO.write(img, "jpg", file);
			
			// Output the total score
			context.write(new Text(key + ".jpg"), new IntWritable(totalScore));
		}
	}
	
	// The Builder class reads all images from a folder, and writes the pixel information to a text-based file
	public static class PixelInfoBuilder {
		private File dir;
		private String[] extensions = new String[]{"jpg", "png", "gif", "bmp"};
		
		// Constructor
		public PixelInfoBuilder(String path) {
			this.dir = new File(path);
		}
		
		// Filter the file type, make sure it reads images only
		public FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for (String ext : extensions) {
	                if (name.endsWith("." + ext)) {
	                    return (true);
	                }
	            }
				return false;
			}
		};
		
		// Build a input file for the Mapper class
		public void build(){
			BufferedImage img = null;
			PrintWriter writer = null;
			String fileName = "", temp = "";
			Color firstColor, nextColor, anotherColor;
			int firstAvgColor = 0, nextAvgColor = 0, anotherAvgColor = 0;
			
			// If the input path is a directory
			if(dir.isDirectory()){
				// Read all images in this folder
				for(File f : dir.listFiles(filter)){
					fileName = f.getName();
					try {
						img = ImageIO.read(f);
						
						System.out.println("\nReading from " + fileName + "...");
						System.out.println("Image size: " + img.getWidth() + "x" + img.getHeight());
						
					} catch (IOException e) {
						System.err.println("Error reading images:");
						e.printStackTrace();
					}
					
					try {
						writer = new PrintWriter("cs755-project-sample//input//" + 
								fileName.replaceFirst("[.][^.]+$", "") + ".txt", "UTF-8");
						
						for (int w = 0; w < img.getWidth(); w++) {
							for (int h = 0; h < img.getHeight(); h++) {

								// Save the RGB values of the pixel at (w,h)
								firstColor = new Color(img.getRGB(w, h));
								// Save the RGB values of the pixel at (w + 1, h)
								nextColor = new Color(img.getRGB((w < (img.getWidth() - 1) ? w + 1 : w), h));
								// Save the RGB values of the pixel at (w, h + 1)
								anotherColor = new Color(img.getRGB(w,(h < (img.getHeight() - 1) ? h + 1 : h)));
 
								// Get the average RGB value of these pixels
								firstAvgColor = ((firstColor.getRed() + firstColor.getGreen() + firstColor.getBlue()) / 3);
								nextAvgColor = ((nextColor.getRed() + nextColor.getGreen() + nextColor.getBlue()) / 3);
								anotherAvgColor = ((anotherColor.getRed() + anotherColor.getGreen() + anotherColor.getBlue()) / 3);
								
								// Write the pixel information to a text file
								temp = (fileName.replaceFirst("[.][^.]+$", "") + "|" + firstAvgColor + "|" + nextAvgColor + "|" + 
											anotherAvgColor + "|" + w + "|" + h);
								writer.println(temp);
							}
						}
						
						writer.close();

					} catch (Exception e) {
						System.err.println("Error writing pixel information:");
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	// Main function
	public static void main(String[] args) {
		Job job;
		
		if(args.length != 2){
			System.err.println("Usage: ConvertImages <input path> <output path>");
			return;
		}
		
		String in = args[0];
		String out =  args[1];
		
		PixelInfoBuilder builder = new PixelInfoBuilder(in);
		builder.build();
		
		System.out.println("Builder completed");
		System.out.println("Starting MapReduce...\n");
		
		try {
			job = new Job();
			job.setJarByClass(ConvertImages.class);
			job.setJobName("ConvertImages");
			
			//Set the path for input and output file
			FileInputFormat.addInputPath(job, new Path(in+"//input"));
			FileOutputFormat.setOutputPath(job, new Path(out));
			
			//Set the Mapper and Reducer class
			job.setMapperClass(Map.class);
			job.setReducerClass(Reduce.class);
			
			//Set the output format for Mapper class
			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(Text.class);
			
			//Set the output format for Reducer class
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(IntWritable.class);
			
			System.exit(job.waitForCompletion(true) ? 0 : 1);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("MapReduce completed\n");
	}
}