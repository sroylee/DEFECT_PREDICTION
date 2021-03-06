public interface VelMethod
{
    /**
     *  invocation method - called when the method invocationshould be
     *  preformed and a value returned
     */
    public Object invoke(Object o, Object[] params)
        throws Exception;

    /**
     *  specifies if this VelMethod is cacheable and able to be
     *  reused for this class of object it was returned for
     *
     *  @return true if can be reused for this class, false if not
     */
    public boolean isCacheable();

    /**
     *  returns the method name used
     */
    public String getMethodName();

    /**
     *  returns the return type of the method invoked
     */
    public Class getReturnType();
}
