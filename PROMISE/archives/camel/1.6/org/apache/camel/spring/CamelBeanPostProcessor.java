package org.apache.camel.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.impl.CamelPostProcessorHelper;
import org.apache.camel.spring.util.ReflectionUtils;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * {@link Endpoint} and
 * {@link org.apache.camel.ProducerTemplate} together with support for
 * {@link org.apache.camel.Consume} and {@link org.apache.camel.MessageDriven} annotations along with
 * {@link org.apache.camel.Produce} annotation along with other annotations such as
 * <p>
 * then one of these bean post processors is implicity installed and configured for you. So you should never have to
 * explicitly create or configure one of these instances.
 *
 * @version $Revision: 707305 $
 */
@XmlRootElement(name = "beanPostProcessor")
@XmlAccessorType(XmlAccessType.FIELD)
public class CamelBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {
    private static final transient Log LOG = LogFactory.getLog(CamelBeanPostProcessor.class);
    @XmlTransient
    private SpringCamelContext camelContext;
    @XmlTransient
    private ApplicationContext applicationContext;
    @XmlTransient
    private CamelPostProcessorHelper postProcessor;

    public CamelBeanPostProcessor() {
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        injectFields(bean);
        injectMethods(bean);
        if (bean instanceof CamelContextAware) {
            CamelContextAware contextAware = (CamelContextAware)bean;
            if (camelContext == null) {
                LOG.warn("No CamelContext defined yet so cannot inject into: " + bean);
            } else {
                contextAware.setCamelContext(camelContext);
            }
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public SpringCamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(SpringCamelContext camelContext) {
        this.camelContext = camelContext;
        postProcessor = new CamelPostProcessorHelper(camelContext) {
            @Override
            protected RuntimeException createProxyInstantiationRuntimeException(Class<?> type, Endpoint endpoint, Exception e) {
                return new BeanInstantiationException(type, "Could not instantiate proxy of type " + type.getName() + " on endpoint " + endpoint, e);
            }
        };
    }


    /**
     * A strategy method to allow implementations to perform some custom JBI
     * based injection of the POJO
     *
     * @param bean the bean to be injected
     */
    protected void injectFields(final Object bean) {
        ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                EndpointInject annotation = field.getAnnotation(EndpointInject.class);
                if (annotation != null) {
                    injectField(field, annotation.uri(), annotation.name(), bean);
                }
                Produce produce = field.getAnnotation(Produce.class);
                if (produce != null) {
                    injectField(field, produce.uri(), produce.ref(), bean);
                }
            }
        });
    }

    protected void injectField(Field field, String endpointUri, String endpointRef, Object bean) {
        ReflectionUtils.setField(field, bean, getPostProcessor().getInjectionValue(field.getType(), endpointUri, endpointRef, field.getName()));
    }

    protected void injectMethods(final Object bean) {
        ReflectionUtils.doWithMethods(bean.getClass(), new ReflectionUtils.MethodCallback() {
            @SuppressWarnings("unchecked")
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                setterInjection(method, bean);
                getPostProcessor().consumerInjection(method, bean);
            }
        });
    }

    protected void setterInjection(Method method, Object bean) {
        EndpointInject annoation = method.getAnnotation(EndpointInject.class);
        if (annoation != null) {
            setterInjection(method, bean, annoation.uri(), annoation.name());
        }
        Produce produce = method.getAnnotation(Produce.class);
        if (produce != null) {
            setterInjection(method, bean, produce.uri(), produce.ref());
        }
    }

    protected void setterInjection(Method method, Object bean, String endpointUri, String endpointRef) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes != null) {
            if (parameterTypes.length != 1) {
                LOG.warn("Ignoring badly annotated method for injection due to incorrect number of parameters: " + method);
            } else {
                String propertyName = ObjectHelper.getPropertyName(method);
                Object value = getPostProcessor().getInjectionValue(parameterTypes[0], endpointUri, endpointRef, propertyName);
                ObjectHelper.invokeMethod(method, bean, value);
            }
        }
    }


    protected void consumerInjection(final Object bean) {
        org.springframework.util.ReflectionUtils.doWithMethods(bean.getClass(), new org.springframework.util.ReflectionUtils.MethodCallback() {
            @SuppressWarnings("unchecked")
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                /*
                 * TODO support callbacks? if
                 * (method.getAnnotation(Callback.class) != null) { try {
                 * Expression e = ExpressionFactory.createExpression(
                 * method.getAnnotation(Callback.class).condition());
                 * JexlContext jc = JexlHelper.createContext();
                 * jc.getVars().put("this", obj); Object r = e.evaluate(jc); if
                 * (!(r instanceof Boolean)) { throw new
                 * RuntimeException("Expression did not returned a boolean value
                 * but: " + r); } Boolean oldVal =
                 * req.getCallbacks().get(method); Boolean newVal = (Boolean) r;
                 * if ((oldVal == null || !oldVal) && newVal) {
                 * req.getCallbacks().put(method, newVal); method.invoke(obj,
                 * the answer } } catch (Exception e) { throw new
                 * RuntimeException("Unable to invoke callback", e); } }
                 */
            }
        });
    }

    public CamelPostProcessorHelper getPostProcessor() {
        ObjectHelper.notNull(postProcessor, "postProcessor");
        return postProcessor;
    }
}
