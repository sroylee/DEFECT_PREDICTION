package org.apache.camel.impl.converter;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.TypeConverter;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.impl.CachingInjector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A {@link TypeConverter} implementation which instantiates an object
 * so that an instance method can be used as a type converter
 *
 * @version $Revision: 525236 $
 */
public class InstanceMethodTypeConverter implements TypeConverter {
    private final CachingInjector injector;
    private final Method method;

    public InstanceMethodTypeConverter(CachingInjector injector, Method method) {
        this.injector = injector;
        this.method = method;
    }

    @Override
    public String toString() {
        return "InstanceMethodTypeConverter: " + method;
    }

    public synchronized <T> T convertTo(Class<T> type, Object value) {
        Object instance = injector.newInstance();
        if (instance == null) {
            throw new RuntimeCamelException("Could not instantiate aninstance of: " + type.getName());
        }
        return (T) ObjectHelper.invokeMethod(method, instance, value);
    }
}