package org.utbot.examples.codegen;

public class ClassWithStaticAndInnerClasses {
    public int z = 0;

    // public field that exposes private type PrivateInnerClassWithPublicField
    public PrivateInnerClassWithPublicField publicFieldWithPrivateType = new PrivateInnerClassWithPublicField(0);

    private static class PrivateStaticClassWithPublicField {
        public int x;

        public PrivateStaticClassWithPublicField(int x) {
            this.x = x;
        }

        public PrivateStaticClassWithPublicField createFromConst(int x) {
            if (x == 0) {
                throw new IllegalArgumentException("Value can't be equal to 0");
            }

            return new PrivateStaticClassWithPublicField(x);
        }
    }

    private static class PrivateStaticClassWithPrivateField {
        private int x;

        public PrivateStaticClassWithPrivateField(int x) {
            this.x = x;
        }

        public PrivateStaticClassWithPrivateField createFromConst(int x) {
            if (x == 0) {
                throw new IllegalArgumentException("Value can't be equal to 0");
            }

            return new PrivateStaticClassWithPrivateField(x);
        }
    }

    public static class PublicStaticClassWithPublicField {
        public int x;

        public PublicStaticClassWithPublicField(int x) {
            this.x = x;
        }

        public PublicStaticClassWithPublicField createFromConst(int x) {
            if (x == 0) {
                throw new IllegalArgumentException("Value can't be equal to 0");
            }

            return new PublicStaticClassWithPublicField(x);
        }
    }

    public static class PublicStaticClassWithPrivateField {

        public static class DeepNestedStatic {
            public int g(int x) {
                return x + 1;
            }
        }

        public class DeepNested {
            public int h(int x) {
                return x + 2;
            }
        }
        private int x;

        public PublicStaticClassWithPrivateField(int x) {
            this.x = x;
        }

        public PublicStaticClassWithPrivateField createFromConst(int x) {
            if (x == 0) {
                throw new IllegalArgumentException("Value can't be equal to 0");
            }

            return new PublicStaticClassWithPrivateField(x);
        }
    }

    private class PrivateInnerClassWithPrivateField {
        private int x;

        public PrivateInnerClassWithPrivateField(int x) {
            this.x = x;
        }

        public PrivateInnerClassWithPrivateField createFromIncrement(int x) {
            if (x == 1) {
                throw new IllegalArgumentException("Value can't be equal to 1");
            }

            return new PrivateInnerClassWithPrivateField(x + 1);
        }
    }

    public class PublicInnerClassWithPrivateField {
        private int x;

        public PublicInnerClassWithPrivateField(int x) {
            this.x = x;
        }

        public PublicInnerClassWithPrivateField createFromIncrement(int x) {
            if (x == 1) {
                throw new IllegalArgumentException("Value can't be equal to 1");
            }

            return new PublicInnerClassWithPrivateField(x + 1);
        }
    }

    private class PrivateInnerClassWithPublicField {
        public int x;

        public PrivateInnerClassWithPublicField(int x) {
            this.x = x;
        }

        public PrivateInnerClassWithPublicField createFromIncrement(int x) {
            if (x == 1) {
                throw new IllegalArgumentException("Value can't be equal to 1");
            }

            return new PrivateInnerClassWithPublicField(x + 1);
        }
    }

    public class PublicInnerClassWithPublicField {
        public int x;

        public PublicInnerClassWithPublicField(int x) {
            this.x = x;
        }

        public PublicInnerClassWithPublicField createFromIncrement(int x) {
            if (x == 1) {
                throw new IllegalArgumentException("Value can't be equal to 1");
            }

            return new PublicInnerClassWithPublicField(x + 1);
        }
    }

    final static class PackagePrivateFinalStaticClassWithPackagePrivateField {
        int x;

        public PackagePrivateFinalStaticClassWithPackagePrivateField(int x) {
            this.x = x;
        }

        public PackagePrivateFinalStaticClassWithPackagePrivateField createFromIncrement(int x) {
            if (x == 1) {
                throw new IllegalArgumentException("Value can't be equal to 1");
            }

            return new PackagePrivateFinalStaticClassWithPackagePrivateField(x + 1);
        }
    }

    final class PackagePrivateFinalInnerClassWithPackagePrivateField {
        int x;

        public PackagePrivateFinalInnerClassWithPackagePrivateField(int x) {
            this.x = x;
        }

        public PackagePrivateFinalInnerClassWithPackagePrivateField createFromIncrement(int x) {
            if (x == 1) {
                throw new IllegalArgumentException("Value can't be equal to 1");
            }

            return new PackagePrivateFinalInnerClassWithPackagePrivateField(x + 1);
        }
    }

    PrivateStaticClassWithPublicField usePrivateStaticClassWithPublicField(int x) {
        PrivateStaticClassWithPublicField privateStaticClassWithPublicField = new PrivateStaticClassWithPublicField(x);

        return privateStaticClassWithPublicField.createFromConst(x);
    }

    PrivateStaticClassWithPrivateField usePrivateStaticClassWithPrivateField(int x) {
        PrivateStaticClassWithPrivateField privateStaticClassWithPrivateField = new PrivateStaticClassWithPrivateField(x);

        return privateStaticClassWithPrivateField.createFromConst(x);
    }

    PublicStaticClassWithPublicField usePublicStaticClassWithPublicField(int x) {
        PublicStaticClassWithPublicField publicStaticClassWithPublicField = new PublicStaticClassWithPublicField(x);

        return publicStaticClassWithPublicField.createFromConst(x);
    }

    PublicStaticClassWithPrivateField usePublicStaticClassWithPrivateField(int x) {
        PublicStaticClassWithPrivateField publicStaticClassWithPrivateField = new PublicStaticClassWithPrivateField(x);

        return publicStaticClassWithPrivateField.createFromConst(x);
    }

    PrivateInnerClassWithPrivateField usePrivateInnerClassWithPrivateField(int x) {
        ClassWithStaticAndInnerClasses classWithStaticAndInnerClasses = new ClassWithStaticAndInnerClasses();
        PrivateInnerClassWithPrivateField innerClass = classWithStaticAndInnerClasses.new PrivateInnerClassWithPrivateField(x);

        return innerClass.createFromIncrement(x);
    }

    PrivateInnerClassWithPublicField usePrivateInnerClassWithPublicField(int x) {
        ClassWithStaticAndInnerClasses classWithStaticAndInnerClasses = new ClassWithStaticAndInnerClasses();
        PrivateInnerClassWithPublicField innerClass = classWithStaticAndInnerClasses.new PrivateInnerClassWithPublicField(x);

        return innerClass.createFromIncrement(x);
    }

    PublicInnerClassWithPrivateField usePublicInnerClassWithPrivateField(int x) {
        ClassWithStaticAndInnerClasses classWithStaticAndInnerClasses = new ClassWithStaticAndInnerClasses();
        PublicInnerClassWithPrivateField innerClass = classWithStaticAndInnerClasses.new PublicInnerClassWithPrivateField(x);

        return innerClass.createFromIncrement(x);
    }

    PublicInnerClassWithPublicField usePublicInnerClassWithPublicField(int x) {
        ClassWithStaticAndInnerClasses classWithStaticAndInnerClasses = new ClassWithStaticAndInnerClasses();
        PublicInnerClassWithPublicField innerClass = classWithStaticAndInnerClasses.new PublicInnerClassWithPublicField(x);

        return innerClass.createFromIncrement(x);
    }

    PackagePrivateFinalStaticClassWithPackagePrivateField usePackagePrivateFinalStaticClassWithPackagePrivateField(int x) {
        PackagePrivateFinalStaticClassWithPackagePrivateField staticClass = new PackagePrivateFinalStaticClassWithPackagePrivateField(x);

        return staticClass.createFromIncrement(x);
    }

    PackagePrivateFinalInnerClassWithPackagePrivateField usePackagePrivateFinalInnerClassWithPackagePrivateField(int x) {
        ClassWithStaticAndInnerClasses classWithStaticAndInnerClasses = new ClassWithStaticAndInnerClasses();
        PackagePrivateFinalInnerClassWithPackagePrivateField innerClass = classWithStaticAndInnerClasses.new PackagePrivateFinalInnerClassWithPackagePrivateField(x);

        return innerClass.createFromIncrement(x);
    }

    int getValueFromPublicFieldWithPrivateType() {
        return publicFieldWithPrivateType.x;
    }
}
