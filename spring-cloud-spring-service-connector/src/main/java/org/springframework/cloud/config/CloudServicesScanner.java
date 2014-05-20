package org.springframework.cloud.config;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.config.java.ServiceScan;
import org.springframework.cloud.service.GenericCloudServiceConnectorFactory;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Bean factory post processor that adds a bean for each service bound to the application.
 *
 * <p>
 * Each service populated by this bean has the same name as the service it is bound to.
 * </p>
 *
 * Usage:
 * Most applications should use either the Java config usign {@link ServiceScan} annotation 
 * or XML config using &lt;cloud:service-scan/&gt; that introduce a bean of this type lgically 
 * equivalent to:
 * <pre>
 * &lt;bean class="org.cloudfoundry.runtime.service.CloudServicesScanner"/&gt;
 * </pre>
 * to have an easy access to all the services.
 *
 * If there is unique bean of a type, you can inject beans using the following
 * code (shows Redis, but the same scheme works for all services):
 * <pre>
 * &#64;Autowired RedisConnectionFactory redisConnectionFactory;
 * </pre>
 *
 * If there are more than one services of a type, you can use the @Qualifier
 * as in the following code:
 * <pre>
 * &#64;Autowired &#64;Qualifier("service-name1") RedisConnectionFactory redisConnectionFactory;
 * &#64;Autowired &#64;Qualifier("service-name2") RedisConnectionFactory redisConnectionFactory;
 * </pre>
 *
 * @author Ramnivas Laddad
 * @author Jennifer Hickey
 *
 */
public class CloudServicesScanner implements ImportBeanDefinitionRegistrar, InitializingBean, BeanFactoryAware {
	private static final String CLOUD_FACTORY_BEAN_NAME = "__cloud_factory__";
	
	private BeanFactory beanFactory = null;
	private Cloud cloud;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) this.beanFactory;
		
		if (cloud == null) {
			if(beanFactory.getBeansOfType(CloudFactory.class).isEmpty()) {
				beanFactory.registerSingleton(CLOUD_FACTORY_BEAN_NAME, new CloudFactory());
			}
			CloudFactory cloudFactory = beanFactory.getBeansOfType(CloudFactory.class).values().iterator().next();
			cloud = cloudFactory.getCloud();
		}
	}

//	@Override
//	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//	}

    private void registerServiceBeans(Cloud cloud, BeanDefinitionRegistry registry) {
		List<ServiceInfo> serviceInfos = cloud.getServiceInfos();
		
		for(ServiceInfo serviceInfo: serviceInfos) {
			registerServiceBean(registry, serviceInfo);
		}
	}
	
	private void registerServiceBean(BeanDefinitionRegistry registry, ServiceInfo serviceInfo) {
		try {
			BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(GenericCloudServiceConnectorFactory.class);
			definitionBuilder.addConstructorArgValue(serviceInfo.getId());
			definitionBuilder.addConstructorArgValue(null);
			registry.registerBeanDefinition(serviceInfo.getId(), definitionBuilder.getBeanDefinition());
		} catch (Exception ex) {
			throw new CloudException("Error registering service factory", ex);
		}
	}

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) throws BeansException {
        registerServiceBeans(cloud, registry);
    }

}
