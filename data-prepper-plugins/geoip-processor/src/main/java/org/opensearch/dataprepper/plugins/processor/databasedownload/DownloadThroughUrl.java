package org.opensearch.dataprepper.plugins.processor.databasedownload;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.opensearch.dataprepper.plugins.processor.configuration.DatabasePathURLConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class DownloadThroughUrl implements DownloadSource {

    public DownloadThroughUrl( ) {  }

    public void initiateDownload(List<DatabasePathURLConfig> urlList)  {

        final File tmpDir = DownloadSource.createFolderIfNotExist(tempFolderPath);
        for(DatabasePathURLConfig url : urlList) {
            DownloadSource.createFolderIfNotExist(tarFolderPath);
            try {
                initiateSSL();
            } catch (Exception ex) {
            }

            buildRequestAndDownloadFile(url.getUrl());
            decompressAndUntarFile(tarFolderPath, downloadTarFilepath, tmpDir);
            deleteTarFolder(tarFolderPath);
        }
    }

    @Override
    public void buildRequestAndDownloadFile(String url)   {
        downloadDBFileFromMaxmind(url, downloadTarFilepath);
    }

    /**
     * decompress And Untar File
     * @param tarFolderPath tar Folder Path
     * @param tarFilepath tar File path
     * @param tmpDir tmp Dir
     */
    private static void decompressAndUntarFile(String tarFolderPath, String tarFilepath, File tmpDir) {
        try {
            final File inputFile = new File(tarFilepath);
            final String outputFile = getFileName(inputFile, tarFolderPath);
            File tarFile = new File(outputFile);
            // Decompress file
            tarFile = deCompressGZipFile(inputFile, tarFile);
            // Untar file
            unTarFile(tarFile, tmpDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * deleteTarFolder
     * @param tarFolder Tar Folder
     */
    private static void deleteTarFolder(String tarFolder) {
        final File file = new File(tarFolder);
        DownloadSource.deleteDirectory(file);
        file.delete();
    }

    /**
     * downloadDBFileFromMaxmind
     * @param maxmindDownloadUrl maxmind Download Url
     * @param tarFilepath tar File path
     */
    private static void downloadDBFileFromMaxmind(String maxmindDownloadUrl, String tarFilepath) {
        try (final BufferedInputStream in = new BufferedInputStream(new URL(maxmindDownloadUrl).openStream());
             final FileOutputStream fileOutputStream = new FileOutputStream(tarFilepath)) {
            final byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) { }
    }

    /**
     * deCompressGZipFile
     * @param gZippedFile  Zipped File
     * @param tarFile tar File
     * @return File
     * @throws IOException io exception
     */
    private static File deCompressGZipFile(File gZippedFile, File tarFile) throws IOException {
        final FileInputStream fileInputStream = new FileInputStream(gZippedFile);
        final GZIPInputStream gZIPInputStream = new GZIPInputStream(fileInputStream);

        final FileOutputStream fileOutputStream = new FileOutputStream(tarFile);
        final byte[] buffer = new byte[1024];
        int len;
        while ((len = gZIPInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, len);
        }
        fileOutputStream.close();
        gZIPInputStream.close();
        return tarFile;
    }

    /**
     * getFileName
     * @param inputFile input File
     * @param outputFolder output Folder
     * @return String
     */
    private static String getFileName(File inputFile, String outputFolder) {
        return outputFolder + File.separator +
                inputFile.getName().substring(0, inputFile.getName().lastIndexOf('.'));
    }

    /**
     * unTarFile
     * @param tarFile tar File
     * @param destFile dest File
     * @throws IOException ioexception
     */
    private static void unTarFile(File tarFile, File destFile) throws IOException {
        final FileInputStream fileInputStream = new FileInputStream(tarFile);
        final TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(fileInputStream);
        TarArchiveEntry tarEntry = null;

        while ((tarEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
            if(tarEntry.getName().endsWith(".mmdb")) {
                String fileName = destFile + File.separator + tarEntry.getName().split("/")[1];
                final File outputFile = new File(fileName);
                if (tarEntry.isDirectory()) {
                    if (!outputFile.exists()) {
                        outputFile.mkdirs();
                    }
                } else {
                    outputFile.getParentFile().mkdirs();
                    final FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    IOUtils.copy(tarArchiveInputStream, fileOutputStream);
                    fileOutputStream.close();
                }
            }
        }
        tarArchiveInputStream.close();
    }
}
