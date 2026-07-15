package com.jrappspot.cashlipi.activities;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import com.jrappspot.cashlipi.R;

public class CalculatorActivity extends BaseActivity {
    private TextView tvExpression, tvResult;
    private StringBuilder expression = new StringBuilder();
    private boolean hasResult = false;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_calculator);
        tvExpression=findViewById(R.id.tvExpression);
        tvResult=findViewById(R.id.tvResult);
        int[] numIds={R.id.btn0,R.id.btn1,R.id.btn2,R.id.btn3,R.id.btn4,R.id.btn5,R.id.btn6,R.id.btn7,R.id.btn8,R.id.btn9};
        String[] nums={"0","1","2","3","4","5","6","7","8","9"};
        for(int i=0;i<numIds.length;i++){final String n=nums[i];findViewById(numIds[i]).setOnClickListener(v->appendChar(n));}
        findViewById(R.id.btnDot).setOnClickListener(v->appendChar("."));
        findViewById(R.id.btnPlus).setOnClickListener(v->appendOperator("+"));
        findViewById(R.id.btnMinus).setOnClickListener(v->appendOperator("-"));
        findViewById(R.id.btnMultiply).setOnClickListener(v->appendOperator("×"));
        findViewById(R.id.btnDivide).setOnClickListener(v->appendOperator("÷"));
        findViewById(R.id.btnPercent).setOnClickListener(v->appendOperator("%"));
        findViewById(R.id.btnEquals).setOnClickListener(v->calculate());
        findViewById(R.id.btnClear).setOnClickListener(v->clear());
        findViewById(R.id.btnBackspace).setOnClickListener(v->backspace());
        findViewById(R.id.btnPlusMinus).setOnClickListener(v->toggleSign());
        updateDisplay();
    }

    private void appendChar(String c){
        if(hasResult){expression=new StringBuilder();hasResult=false;}
        if(c.equals(".")&&expression.toString().contains(".")){return;}
        expression.append(c);updateDisplay();
    }

    private void appendOperator(String op){
        hasResult=false;
        if(expression.length()==0&&!op.equals("-"))return;
        char last=expression.length()>0?expression.charAt(expression.length()-1):' ';
        if("+-×÷%".indexOf(last)>=0)expression.deleteCharAt(expression.length()-1);
        expression.append(op);updateDisplay();
    }

    private void calculate(){
        try{
            String expr=expression.toString().replace("×","*").replace("÷","/");
            if(expr.isEmpty())return;
            // Handle % operator
            expr=expr.replaceAll("(\\d+(?:\\.\\d+)?)%","($1/100)");
            double result=evalExpr(expr);
            tvExpression.setText(expression.toString()+" =");
            if(result==(long)result) tvResult.setText(String.valueOf((long)result));
            else tvResult.setText(String.format("%.4f",result).replaceAll("0+$","").replaceAll("\\.$",""));
            expression=new StringBuilder(tvResult.getText().toString());
            hasResult=true;
        }catch(Exception e){tvResult.setText("Error");expression=new StringBuilder();}
    }

    private double evalExpr(String expr) throws Exception {
        // Simple eval using javax.script or manual parsing
        // Use stack-based evaluation
        java.util.Stack<Double> nums=new java.util.Stack<>();
        java.util.Stack<Character> ops=new java.util.Stack<>();
        int i=0;
        while(i<expr.length()){
            char c=expr.charAt(i);
            if(Character.isDigit(c)||c=='.'){
                StringBuilder sb=new StringBuilder();
                while(i<expr.length()&&(Character.isDigit(expr.charAt(i))||expr.charAt(i)=='.'))sb.append(expr.charAt(i++));
                nums.push(Double.parseDouble(sb.toString()));continue;
            }
            if(c=='('){ ops.push(c);i++;continue;}
            if(c==')'){while(ops.peek()!='(')nums.push(applyOp(ops.pop(),nums.pop(),nums.pop()));ops.pop();i++;continue;}
            if(c=='+'||c=='-'||c=='*'||c=='/'){
                while(!ops.empty()&&hasPrecedence(c,ops.peek()))nums.push(applyOp(ops.pop(),nums.pop(),nums.pop()));
                ops.push(c);i++;continue;
            }
            i++;
        }
        while(!ops.empty())nums.push(applyOp(ops.pop(),nums.pop(),nums.pop()));
        return nums.pop();
    }

    private boolean hasPrecedence(char op1,char op2){
        if(op2=='('||op2==')')return false;
        if((op1=='*'||op1=='/')&&(op2=='+'||op2=='-'))return false;
        return true;
    }

    private double applyOp(char op,double b,double a){
        switch(op){case '+':return a+b;case '-':return a-b;case '*':return a*b;case '/':if(b==0)throw new ArithmeticException("Divide by zero");return a/b;}
        return 0;
    }

    private void clear(){expression=new StringBuilder();hasResult=false;tvExpression.setText("");tvResult.setText("0");}
    private void backspace(){if(expression.length()>0){expression.deleteCharAt(expression.length()-1);updateDisplay();}}
    private void toggleSign(){
        if(expression.length()>0&&expression.charAt(0)=='-')expression.deleteCharAt(0);
        else expression.insert(0,'-');
        updateDisplay();
    }
    private void updateDisplay(){
        String expr=expression.toString();
        tvExpression.setText(expr.isEmpty()?"":expr);
        if(!hasResult)tvResult.setText(expr.isEmpty()?"0":expr);
    }
}
