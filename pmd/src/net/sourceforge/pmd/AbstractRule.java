/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd;

import net.sourceforge.pmd.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.ast.ASTCompilationUnit;
import net.sourceforge.pmd.ast.ASTMethodDeclarator;
import net.sourceforge.pmd.ast.JavaParserVisitorAdapter;
import net.sourceforge.pmd.ast.SimpleNode;
import net.sourceforge.pmd.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.symboltable.Scope;
import net.sourceforge.pmd.symboltable.MethodScope;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public abstract class AbstractRule extends JavaParserVisitorAdapter implements Rule {

    protected String name = getClass().getName();
    protected Properties properties = new Properties();
    protected String message;
    protected String description;
    protected String example;
    protected String ruleSetName;
    protected boolean include;
    protected boolean usesDFA;
    protected int priority = LOWEST_PRIORITY;

    public String getRuleSetName() {
        return ruleSetName;
    }

    public void setRuleSetName(String ruleSetName) {
        this.ruleSetName = ruleSetName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public void addProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public void addProperties(Properties properties) {
        this.properties.putAll(properties);
    }

    public double getDoubleProperty(String name) {
        return Double.parseDouble(properties.getProperty(name));
    }

    public int getIntProperty(String name) {
        return Integer.parseInt(properties.getProperty(name));
    }

    public boolean getBooleanProperty(String name) {
        return Boolean.valueOf(properties.getProperty(name)).booleanValue();
    }

    public String getStringProperty(String name) {
        return properties.getProperty(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Rule)) {
            return false;
        }
        return ((Rule) o).getName().equals(getName());
    }

    public int hashCode() {
        return getName().hashCode();
    }

    protected void visitAll(List acus, RuleContext ctx) {
        for (Iterator i = acus.iterator(); i.hasNext();) {
            ASTCompilationUnit node = (ASTCompilationUnit) i.next();
            visit(node, ctx);
        }
    }

    public void apply(List acus, RuleContext ctx) {
        visitAll(acus, ctx);
    }


    public RuleViolation createRuleViolation(RuleContext ctx, SimpleNode node) {
        String packageName = node.getScope().getEnclosingSourceFileScope().getPackageName() == null ? "" : node.getScope().getEnclosingSourceFileScope().getPackageName();
        String className = findClassName(node);
        String methodName = findMethodName(node);
        RuleViolation v = new RuleViolation(this, ctx, packageName, className, methodName);
        extractNodeInfo(v, node);
        return v;
    }

    public RuleViolation createRuleViolation(RuleContext ctx, SimpleNode node, String specificDescription) {
        String packageName = node.getScope().getEnclosingSourceFileScope().getPackageName() == null ? "" : node.getScope().getEnclosingSourceFileScope().getPackageName();
        String className = findClassName(node);
        String methodName = findMethodName(node);
        RuleViolation rv = new RuleViolation(this, node.getBeginLine(), specificDescription, ctx, packageName, className, methodName);
        extractNodeInfo(rv, node);
        return rv;
    }

    public RuleViolation createRuleViolation(RuleContext ctx, SimpleNode node, String variableName, String specificDescription) {
        String packageName = node.getScope().getEnclosingSourceFileScope().getPackageName() == null ? "" : node.getScope().getEnclosingSourceFileScope().getPackageName();
        String className = findClassName(node);
        String methodName = findMethodName(node);
        return new RuleViolation(this, node.getBeginLine(), node.getEndLine(), variableName, specificDescription, ctx, packageName, className, methodName);
    }

    private String findMethodName(SimpleNode node) {
        String methodName;
        if (node.getFirstParentOfType(ASTMethodDeclaration.class) == null) {
            return "";
        } else {
            Scope s = node.getScope();
            while (!(s instanceof MethodScope)) {
                s = s.getParent();
            }
            MethodScope ms = (MethodScope)s;
            methodName = ms.getName();
        }
        return methodName;
    }

    private String findClassName(SimpleNode node) {
        String className;
        if (node.getFirstParentOfType(ASTClassOrInterfaceDeclaration.class) == null) {
            // This takes care of nodes which are outside a class definition - i.e., import declarations
            className = "";
        } else {
             // default to symbol table lookup
            className = node.getScope().getEnclosingClassScope().getClassName() == null ? "" : node.getScope().getEnclosingClassScope().getClassName();
        }
        return className;
    }

    public Properties getProperties() {
        return properties;
    }

    public boolean include() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public int getPriority() {
        return priority;
    }

    public String getPriorityName() {
        return PRIORITIES[getPriority() - 1];
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setUsesDFA() {
        this.usesDFA = true;
    }

    public boolean usesDFA() {
        return this.usesDFA;
    }

    /**
     * Adds a violation to the report.
     * 
     * @param context the RuleContext
     * @param node the node that produces the violation, may be null, in which case all line and column info will be set to zero
     */
    protected final void addViolation(RuleContext context, SimpleNode node) {
        context.getReport().addRuleViolation(createRuleViolation(context, node));
    }

    /**
     * Gets the Image of the first parent node of type ASTClassOrInterfaceDeclaration or <code>null</code>
     *  
     * @param node the node which will be searched
     * @return
     */
    protected final String getDeclaringType(SimpleNode  node) {
		ASTClassOrInterfaceDeclaration c = (ASTClassOrInterfaceDeclaration) node.getFirstParentOfType(ASTClassOrInterfaceDeclaration.class);
		if (c!=null)
			return c.getImage();
		return null;
	}

    private final void extractNodeInfo(RuleViolation v, SimpleNode n) {
        if (n==null) {
            v.setLine(0);
            v.setColumnInfo(0, 0);
        } else {
            v.setLine(n.getBeginLine());
            v.setColumnInfo(n.getBeginColumn(), n.getEndColumn());
        }
    }

}
