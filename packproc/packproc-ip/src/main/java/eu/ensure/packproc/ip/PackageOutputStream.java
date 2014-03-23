package eu.ensure.packproc.ip;

import eu.ensure.packproc.model.StructureEntry;
import eu.ensure.packproc.model.StructureOutputStream;
import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PackageOutputStream extends StructureOutputStream {

	private ArchiveOutputStream outputStream = null;
	
    public PackageOutputStream(ArchiveOutputStream aos) throws IllegalArgumentException {
		if (null == aos) {
			throw new IllegalArgumentException("can not operate on null output stream");
		}
        outputStream = aos;
    }

    /*
     * These are StructureOutputStream methods
     */
    public void copyEntry(StructureEntry entry) throws IOException {
        if (entry instanceof PackageEntry) {
            outputStream.putArchiveEntry((ArchiveEntry)entry.getWrappedObject());
        } else {
            throw new IOException("Incompatible entry: " + entry.getClass().getName());
        }
    }

    public void replaceEntry(StructureEntry entry, File file) throws IOException {
        if (entry instanceof PackageEntry) {
            // Well, at least we have encapsulated these specifics in this adapter
            ArchiveEntry replacement = null;
            if (outputStream instanceof TarArchiveOutputStream) {
                replacement = new TarArchiveEntry(file, entry.getName());
            }
            else if (outputStream instanceof JarArchiveOutputStream) {
                JarArchiveEntry jarrr = new JarArchiveEntry(entry.getName());
                jarrr.setSize(file.length());
                replacement = jarrr;
            }
            else if (outputStream instanceof ZipArchiveOutputStream) {
                ZipArchiveEntry zipe = new ZipArchiveEntry(entry.getName());
                zipe.setSize(file.length());
                replacement = zipe;
            }
            else {
                String info = "Cannot write entries to: " + outputStream.getClass().getName();
                Exception syntheticException = new Exception(info); // just to get a stack trace
                throw new UnsupportedOperationException(info, syntheticException);
            }
            outputStream.putArchiveEntry(replacement);
        } else {
            throw new IOException("Incompatible entry: " + entry.getClass().getName());
        }
    }

    public void closeEntry() throws IOException {
        outputStream.closeArchiveEntry();
    }

    /*
     * Here follows standard OutputStream methods
     */
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    public void write(byte b[]) throws IOException {
        outputStream.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    public void flush() throws IOException {
        outputStream.flush();
    }

    public void close() throws IOException {
        outputStream.flush();
    }

    /*
     * Handle creation of ArchiveOutputStream that matches any given ArchiveInputStream in the case that we do not
     * know the type of the input stream ahead of opening it.
     */
    private static final ArchiveStreamFactory factory = new ArchiveStreamFactory();

    private static final String RE = "org[.]apache[.]commons[.]compress[.]archivers[.]([\\w]+)[.].*?InputStream";
    private static final Pattern PATTERN = Pattern.compile(RE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static PackageOutputStream createOutputStreamFrom(ArchiveInputStream is, OutputStream os) throws ArchiveException {
        // We will only wrap existing output streams
        if (null == os) {
            return null;
        }

        Matcher m = PATTERN.matcher(is.getClass().getName());
        if (m.find())
        {
            String archiver = m.group(1);
            return new PackageOutputStream(factory.createArchiveOutputStream(archiver, os));
        }

        // As some kind of default, create a TAR archive
        return new PackageOutputStream(factory.createArchiveOutputStream(ArchiveStreamFactory.TAR, os));
    }
}
