package de.keksuccino.core.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.io.Files;

public class FileUtils {

	//TODO übernehmen
	public static void writeTextToFile(File f, boolean append, String... text) throws IOException {
		FileOutputStream fo = new FileOutputStream(f, append);
		OutputStreamWriter os = new OutputStreamWriter(fo, StandardCharsets.UTF_8);
		BufferedWriter writer = new BufferedWriter(os);
        if (text.length == 1) {
        	writer.write(text[0]);
        } else if (text.length > 0) {
        	for (String s : text) {
        		writer.write(s + "\n");
        	}
        }
        writer.flush();
        try {
        	if (writer != null) {
        		writer.close();
        	}
        	if (fo != null) {
        		fo.close();
        	}
        	if (os != null) {
        		os.close();
        	}
		} catch (Exception e) {}
	}
	
	public static List<String> getFileLines(File f) {
		List<String> list = new ArrayList<>();
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));
			String line = in.readLine();
			while (line != null) {
				list.add(line);
				line = in.readLine();
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	public static List<String> getFiles(String path) {
		List<String> list = new ArrayList<>();
		File f = new File(path);
		if (f.exists()) {
			for (File file : f.listFiles()) {
				list.add(file.getAbsolutePath());
			}
		}
		
		return list;
	}
	
	public static List<String> getFilenames(String path, boolean includeExtension) {
		List<String> list = new ArrayList<>();
		File f = new File(path);
		if (f.exists()) {
			for (File file : f.listFiles()) {
				if (includeExtension) {
					list.add(file.getName());
				} else {
					list.add(Files.getNameWithoutExtension(file.getName()));
				}
			}
		}
		
		return list;
	}
	
	//TODO übernehmen
	public static String generateAvailableFilename(String dir, String baseName, String extension) {
		File f = new File(dir);
		if (!f.exists() && f.isDirectory()) {
			f.mkdirs();
		}

		File f2 = new File(f.getPath() + "/" + baseName + "." + extension.replace(".", ""));
		int i = 1;
		while (f2.exists()) {
			f2 = new File(f.getPath() + "/" + baseName + "_" + i + "." + extension.replace(".", ""));
			i++;
		}
		
		return f2.getName();
	}
	
	public static void compressToZip(String pathToCompare, String zipFile) {
        byte[] buffer = new byte[1024];
        String source = new File(pathToCompare).getName();
		FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);
            
            for (String file: getFiles(pathToCompare)) {
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);
                try {
                	FileInputStream in = new FileInputStream(file);
                    int len;
                    while ((len = in .read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    in.close();
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
            
            zos.closeEntry();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                zos.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
	
	public static void compressToZip(List<String> filePathsToCompare, String zipFile) {
        byte[] buffer = new byte[1024];

        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);
            for (String file: filePathsToCompare) {
                ZipEntry ze = new ZipEntry(Files.getNameWithoutExtension(zipFile) + "/" + file);
                zos.putNextEntry(ze);
                try {
                	FileInputStream in = new FileInputStream(file);
                    int len;
                    while ((len = in .read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    in.close();
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
            zos.closeEntry();

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	
}
