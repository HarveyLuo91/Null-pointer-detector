package example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileIO3 {
    static FileInputStream fos = null;

    public static void main(String[] args) throws IOException {
        test();
        pass();

    }

    public static void test() {
//
//        FileIO3 f3 = new FileIO3();
//
//        try {
//            f3.fos = new FileInputStream(new File("test.txt"));
//            f3.fos = ok();
//            f3.fos.read();
//        } catch (FileNotFoundException e) {
//
//        } finally {
//            if (f3.fos != null)
//                f3.fos.close();
//        }

    }

    public static void pass() {
        FileInputStream x = fos;
        pass1(x);

    }

    public static void pass1(FileInputStream file) {
        try {
            file.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static FileInputStream ok() {
        try {
            return new FileInputStream(new File("test.txt"));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
