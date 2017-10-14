package example;

public class NullPointerCase1 {

    public static void main(String[] args) {

//        NullPointerCase3 b = null;
        foo();
//        b.test(1);

//        foo();
//        System.out.println(a);
    }

    public static void foo() {
        int a;
        NullPointerCase2 b = new NullPointerCase2();
        NullPointerCase3 io = null;
        String c;

        a = 1;

        b.foo(io);

        io.test();

        System.out.println(a);

        c = "hello";
    }
}
