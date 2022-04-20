package com.sun.tools.javac.file;

import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;

public class AndroidFsProvider {

    public static AndroidFsProvider INSTANCE = new AndroidFsProvider();

    public FileSystemProvider zipFsProvider () {
        throw new UnsupportedOperationException("Not implemented!");
    }

    public FileSystem jrtFileSystem () {
        throw new UnsupportedOperationException("Not implemented!");
    }
}