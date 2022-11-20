package com.amansprojects.foliage;

import com.amansprojects.foliage.antlr.FoliageBaseListener;
import com.amansprojects.foliage.antlr.FoliageParser;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ClassListener extends FoliageBaseListener {
	private final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	public List<Method> methods = new ArrayList<Method>();
	
	public ClassListener() {
		super();
		cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "GeneratedClass", null, "java/lang/Object", null);
	}

    interface Statement {
        public void invoke(MethodVisitor v);
    }

    enum Operator {
        ADD,
        SUB,
        MUL,
        DIV
    }

    interface Operation<T> {
        public void value(T value);
        public void operator(Operator operator);
    }

    static class IntOperation implements Statement, Operation<Integer> {
        private List<Integer> values = new ArrayList<Integer>();
        private List<Operator> operators = new ArrayList<Operator>();

        public void value(Integer i) {
            values.add(i);
        }

        public void operator(Operator o) {
            operators.add(o);
        }

        public void invoke(MethodVisitor v) {
            v.visitLdcInsn(values.get(0));
            for (int i = 0; i < operators.size(); i++) {
                v.visitLdcInsn(values.get(i + 1));
                switch (operators.get(i)) {
                    case ADD -> v.visitInsn(Opcodes.IADD);
                    case SUB -> v.visitInsn(Opcodes.ISUB);
                    case MUL -> v.visitInsn(Opcodes.IMUL);
                    case DIV -> v.visitInsn(Opcodes.IDIV);
                }
            }
        }
    }

    static class FloatOperation implements Statement, Operation<Float> {
        private List<Float> values = new ArrayList<Float>();
        private List<Operator> operators = new ArrayList<Operator>();

        public void value(Float f) {
            values.add(f);
        }

        public void operator(Operator o) {
            operators.add(o);
        }

        public void invoke(MethodVisitor v) {
            v.visitLdcInsn(values.get(0));
            for (int i = 0; i < operators.size(); i++) {
                v.visitLdcInsn(values.get(i + 1));
                switch (operators.get(i)) {
                    case ADD -> v.visitInsn(Opcodes.FADD);
                    case SUB -> v.visitInsn(Opcodes.FSUB);
                    case MUL -> v.visitInsn(Opcodes.FMUL);
                    case DIV -> v.visitInsn(Opcodes.FDIV);
                }
            }
        }
    }

    static class Method {
        List<Statement> statements = new ArrayList<Statement>();
        Statement st;

        String returnValue = "";
        int returnType;
    }

    private Method method;

    @Override
    public void enterMethod(FoliageParser.MethodContext ctx) {
        methods.add(new Method());
        method = methods.get(methods.size() - 1);
    }

    @Override
    public void enterIntOperation(FoliageParser.IntOperationContext ctx) {
		method.statements.add(new IntOperation());
        method.st = method.statements.get(method.statements.size() - 1);
    }

    @Override
    public void exitInteger(FoliageParser.IntegerContext ctx) {
		if (ctx.getText().isEmpty()) return;
        if (method.st instanceof IntOperation op) {
            op.values.add(Integer.parseInt(ctx.getText()));
        }
    }

    @Override
    public void enterFloatOperation(FoliageParser.FloatOperationContext ctx) {
        method.statements.add(new FloatOperation());
        method.st = method.statements.get(method.statements.size() - 1);
    }

    @Override
    public void exitFloat(FoliageParser.FloatContext ctx) {
        if (ctx.getText().isEmpty()) return;
        if (method.st instanceof FloatOperation op) {
            op.values.add(Float.parseFloat(ctx.getText()));
        }
    }

    @Override
    public void exitOperator(FoliageParser.OperatorContext ctx) {
        if (method.st instanceof Operation<?> op) {
            switch (ctx.getText()) {
                case "+" -> op.operator(Operator.ADD);
                case "-" -> op.operator(Operator.SUB);
                case "*" -> op.operator(Operator.MUL);
                case "/" -> op.operator(Operator.DIV);
            }
        }
    }

    @Override
    public void exitReturn(FoliageParser.ReturnContext ctx) {
        method.returnValue = ctx.value.getText();
    }

    @Override
    public void exitMethod(FoliageParser.MethodContext ctx) {
        switch (ctx.type.getText()) {
            case "void" -> method.returnType = Type.VOID;
            case "int" -> method.returnType = Type.INT;
            case "float" -> method.returnType = Type.FLOAT;
        }
        String descriptor = "()%RETURN";
        switch (method.returnType) {
            case Type.VOID -> descriptor = descriptor.replaceFirst("%RETURN", "V");
            case Type.INT -> descriptor = descriptor.replaceFirst("%RETURN", "I");
            case Type.FLOAT -> descriptor = descriptor.replaceFirst("%RETURN", "F");
        }
        MethodVisitor v = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, ctx.name.getText(), descriptor, null, null);
        for (Statement st : method.statements) {
            st.invoke(v);
        }
        if (method.returnValue.isEmpty()) {
            switch (method.returnType) {
                case Type.VOID -> v.visitInsn(Opcodes.RETURN);
                case Type.INT -> v.visitInsn(Opcodes.IRETURN);
                case Type.FLOAT -> v.visitInsn(Opcodes.FRETURN);
            }
        } else {
            switch (method.returnType) {
                case Type.VOID -> v.visitInsn(Opcodes.RETURN);
                case Type.INT -> {
                    v.visitLdcInsn(Integer.parseInt(method.returnValue));
                    v.visitInsn(Opcodes.IRETURN);
                }
                case Type.FLOAT -> {
                    v.visitLdcInsn(Float.parseFloat(method.returnValue));
                    v.visitInsn(Opcodes.FRETURN);
                }
            }
        }

        v.visitMaxs(0, 0);
        v.visitEnd();
    }

	@Override
	public void exitProgram(FoliageParser.ProgramContext ctx) {
		cw.visitEnd();
		
		try (FileOutputStream stream = new FileOutputStream("target/classes/GeneratedClass.class")) {
            stream.write(cw.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}