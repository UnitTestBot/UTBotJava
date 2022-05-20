public class NoPackageClassSample {
    public NoPackageClassSample() {
    }

    public NoPackageClassSample(NoPackageClassSample other) {
    }

    public void multipleNoPackageArgs(NoPackageClassSample a, NoPackageClassSample b) {
    }

    public void mixedArgs(NoPackageClassSample a, int b, Object c) {
    }

    public NoPackageClassSample returnsNoPackageNoArgs() {
        return this;
    }

    public NoPackageClassSample returnsNoPackage(NoPackageClassSample arg) {
        return arg;
    }
}
