package com.sun.tools.javac.file;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;

import com.itsaky.androidide.zipfs.ZipFileSystemProvider;
import jdk.internal.jrtfs.JrtFileSystemProvider;

public class AndroidFsProvider {

    // Can be overridden to provide custom implementation
    public static AndroidFsProvider INSTANCE = new AndroidFsProvider();

    public FileSystemProvider zipFsProvider () {
        return new ZipFileSystemProvider();
    }

    public FileSystem jrtFileSystem () {
        try {
            return new JrtFileSystemProvider().newFileSystem(URI.create("jrt:/"), new HashMap<>(0));
        } catch (IOException e) {
            throw new RuntimeException ("Unable to create instance of JRTFileSystem", e);
        }
    }
}