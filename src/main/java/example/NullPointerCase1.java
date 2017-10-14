package example;

public class NullPointerCase1 {

    public static void main(String[] args) {
        foo();
//        System.out.println(a);
    }

    public static void foo() {
        int a;
        NullPointerCase2 b;
        FileIO3 io;
        String c;

        a = 1;

//        b.foo(io);

        io.test();

        System.out.println(a);

        c = "hello";
    }
}
