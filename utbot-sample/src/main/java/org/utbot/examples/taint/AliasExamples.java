package org.utbot.examples.taint;

import org.utbot.examples.taint.aliases.AliasA;
import org.utbot.examples.taint.aliases.AliasB;

import static org.utbot.examples.taint.BadSink.writeIntoBd;

public class AliasExamples {

    public void bad550() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        writeIntoBd(a.f.getData());
    }

    public void paramDependentBad() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        BadSink.onlySecondParamIsImportant("safe", a.f.getData());
    }

    public void clearSecondParameter() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        BadSink.onlySecondParamIsImportant("safe", TaintCleaner.removeTaintMark(a.f.getData()));
    }

    public void paramDependentGood() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        BadSink.onlySecondParamIsImportant(a.f.getData(), "safe");
    }

    public void passSecondParamGood() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        String param = a.f.getData();
        TaintPassThrough taintPassThrough = new TaintPassThrough();

        BadSink.writeIntoBd(taintPassThrough.passSecondParameter(param, ""));
    }

    public void passSecondParamBad() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        String param = a.f.getData();
        TaintPassThrough taintPassThrough = new TaintPassThrough();

        BadSink.writeIntoBd(taintPassThrough.passSecondParameter("", param));
    }

    public void passFirstParamGood() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        String param = a.f.getData();
        TaintPassThrough taintPassThrough = new TaintPassThrough();

        BadSink.writeIntoBd(taintPassThrough.passFirstParameter("", param));
    }

    public void passFirstParamBad() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        String param = a.f.getData();
        TaintPassThrough taintPassThrough = new TaintPassThrough();

        BadSink.writeIntoBd(taintPassThrough.passFirstParameter(param, ""));
    }

    public void passSecondParam() {
        AliasA a = new AliasA();
        AliasA b = new AliasA();
        b.getF().setData(BadSource.getEnvironment("data"));

        a.f = b.f;

        BadSink.onlySecondParamIsImportant(a.f.getData(), "safe");
    }

    public void bad551() {
        AliasA a = new AliasA();
        a.getF().setData(BadSource.getEnvironment("data"));

        AliasA b = a;

        writeIntoBd(b.getF().getData());
    }

    public void bad552() {
        AliasB b = new AliasB();
        b.setData(BadSource.getEnvironment("data"));
        AliasA a = new AliasA(b);
        AliasA c = new AliasA();
        assign(a, c);
        AliasB d = c.f;

        writeIntoBd(d.getData());
    }

    private static void assign(AliasA x, AliasA y) {
        y.f = x.f;
    }

    public void bad553() {
        AliasB b = new AliasB();
        b.setData(BadSource.getEnvironment("data"));
        AliasA a = new AliasA(b);
        AliasA c = new AliasA();
        assign(a, c);
        AliasB d = c.f;

        writeIntoBd(d.getData());
    }

    public void bad554() {
        AliasA a = new AliasA();
        a.setData(BadSource.getEnvironment("data"));
        AliasA b = new AliasA();

        b = a;

        writeIntoBd(b.getData());
    }

}
