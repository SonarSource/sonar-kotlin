package androidx.annotation;

// Local stub of androidx.annotation.VisibleForTesting, mirroring its public API,
// so sample files can compile without pulling in the real androidx dependency.
public @interface VisibleForTesting {

    int otherwise() default PRIVATE;

    int PROTECTED = 4;
    int PACKAGE_PRIVATE = 3;
    int PRIVATE = 2;
    int NONE = 5;
}
