import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Vector;

public class ThreadServer {

//mainclass

	public static void main(String[] args) {

		if (args.length < 2 || args.length > 2) {
			System.out.println("No of Arguments : 2 ");
			System.out.println("Format : <URL> <Number of connections>");
			return;
		}

		String url = args[0];
		String n = args[1];

		int noconnection = Integer.parseInt(n);

		for (int i = 1; i <= noconnection; i++) {
			Thread1 obj1 = new Thread1(url, noconnection, i); // Creating Threads
			Thread object = new Thread(obj1);

			object.start();

		}

	}
}

//Thread implmentation

class Thread1 implements Runnable {
	String url;

	int noconnection;
	int i, dividelength, length = 0;

	// combining all the files

	public void combineFiles_and_comparesha256() throws IOException {
		String originalfile = "";
		String combinedfile = "";
		File file1 = new File("Combined_file");
		File file2 = new File("originalFile_for_sha");
		System.out.println("Combining all separate files into one... ");
		Vector<Object> vector1 = new Vector<Object>();
		for (int j = 1; j <= noconnection; j++) {

			FileInputStream input = new FileInputStream("parts_" + j);
			vector1.addElement(input);
		}

		@SuppressWarnings("rawtypes")
		Enumeration enum1 = vector1.elements();
		@SuppressWarnings("unchecked")
		SequenceInputStream s = new SequenceInputStream(enum1);
		FileOutputStream output = new FileOutputStream(file1);

		int a;
		while ((a = s.read()) != -1) {

			output.write(a); // prints in the destination file
		}
		output.close();
		s.close();

		// Calling the sha function for the combined file

		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			combinedfile = checkFiles(file1, sha);
		} catch (Exception e) {
			System.out.println(e);
		}
		System.out.println(" Files Are combined into one single file : " + file1);
		System.out.println("sha256 hashed value of combined file : " + combinedfile);
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			InputStream input = con.getInputStream();
			OutputStream out = new FileOutputStream(file2);
			int rw = 0;

			while (rw != -1) {
				rw = input.read();

				if (rw != -1)
					out.write(rw);

			}
			out.close();
			input.close();

			// Calling the sha function for the original file

			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				originalfile = checkFiles(file2, md);
			} catch (Exception e) {
				System.out.println(e);
			}

			System.out.println("sha256 hashed value of original file from url : " + originalfile);

			length = con.getContentLength();
			con.disconnect();

		} catch (MalformedURLException m) {
			System.out.println(m);
		} catch (IOException e) {
			System.out.println(e);
		}
		if (originalfile.equals(combinedfile)) {
			System.out.println("The files have been compared using sha256 and they are equal");
		} else {
			System.out.println("The files have been compared using sha256 and they are not equal");
		}
	}

//Computing sha256 hex value

	public static String checkFiles(File filepath, MessageDigest md) throws NoSuchAlgorithmException, IOException {
		try (DigestInputStream d = new DigestInputStream(new FileInputStream(filepath), md)) {
			while (d.read() != -1)
				; // empty loop to clear the data
			md = d.getMessageDigest();
		}

		// bytes to hex conversion

		StringBuilder sbuilder = new StringBuilder();
		for (byte b : md.digest()) {
			sbuilder.append(String.format("%02x", b));
		}

		String value = sbuilder.toString();
		return value;
	}

	// Thread constructor

	public Thread1(String url, int noconnection, int i) {
		this.url = url;
		this.noconnection = noconnection;
		this.i = i;

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			length = con.getContentLength();
			con.disconnect();

		} catch (MalformedURLException m) {
			System.out.println(m);
		} catch (IOException e) {
			System.out.println(e);
		}

		dividelength = length / noconnection;
	}

	// Run method of Threads

	public void run() {
		int ub;
		int lb;
		int n;
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			if (length % noconnection == 0) {
				ub = (dividelength * i) - 1; // Calculating the byte range for individual parts
				lb = (ub - dividelength) + 1;
			} else {
				n = length % noconnection;
				if (i == noconnection) {
					int temp;
					temp = ((dividelength * i) - 1);
					ub = temp + n;
					lb = (temp - dividelength) + 1;
				} else {
					ub = (dividelength * i) - 1;
					lb = (ub - dividelength) + 1;
				}
			}
			String bound = lb + "-" + ub;
			System.out.println("Byte range of file parts_" + i);
			System.out.println(bound);
			con.setRequestProperty("Range", "Bytes=" + bound); // Range Request for the particular byte range
			con.connect();

			InputStream input = con.getInputStream();
			OutputStream output = new FileOutputStream(new File("parts_" + i));
			int rw = 0;
			while (rw != -1) {
				rw = input.read();

				if (rw != -1)
					output.write(rw); // Writing into the parts_i file
			}
			output.close();
			con.disconnect();
			if (i == noconnection) {

				combineFiles_and_comparesha256(); // Calling the function to combine files
			}

		}

		catch (MalformedURLException m) {
			System.out.println(m);
		} catch (IOException e) {
			System.out.println(e);
		}

	}
}
