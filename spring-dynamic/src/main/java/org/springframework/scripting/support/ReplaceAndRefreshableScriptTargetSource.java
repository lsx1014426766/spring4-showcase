/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scripting.support;

import org.springframework.aop.target.dynamic.BeanFactoryRefreshableTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Subclass of {@link BeanFactoryRefreshableTargetSource} that determines whether
 * a refresh is required through the given {@link ScriptFactory}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 */
public class ReplaceAndRefreshableScriptTargetSource extends RefreshableScriptTargetSource {

    private final ScriptFactory scriptFactory;

    private final ScriptSource scriptSource;

    private final boolean isFactoryBean;


    /**
     * Create a new RefreshableScriptTargetSource.
     *
     * @param beanFactory   the BeanFactory to fetch the scripted bean from
     * @param beanName      the name of the target bean
     * @param scriptFactory the ScriptFactory to delegate to for determining
     *                      whether a refresh is required
     * @param scriptSource  the ScriptSource for the script definition
     * @param isFactoryBean whether the target script defines a FactoryBean
     */
    public ReplaceAndRefreshableScriptTargetSource(BeanFactory beanFactory, String beanName,
                                                   ScriptFactory scriptFactory, ScriptSource scriptSource, boolean isFactoryBean) {

        super(beanFactory, beanName, scriptFactory, scriptSource, isFactoryBean);
        Assert.notNull(scriptFactory, "ScriptFactory must not be null");
        Assert.notNull(scriptSource, "ScriptSource must not be null");
        this.scriptFactory = scriptFactory;
        this.scriptSource = scriptSource;
        this.isFactoryBean = isFactoryBean;
    }


    /**
     * Determine whether a refresh is required through calling
     * ScriptFactory's {@code requiresScriptedObjectRefresh} method.
     *
     * @see ScriptFactory#requiresScriptedObjectRefresh(ScriptSource)
     */
    @Override
    protected boolean requiresRefresh() {
        return this.scriptFactory.requiresScriptedObjectRefresh(this.scriptSource);
    }

    /**
     * Obtain a fresh target object, retrieving a FactoryBean if necessary.
     */
    @Override
    protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
        removeInjectCache(beanFactory, beanName);

        return super.obtainFreshBean(beanFactory,
                (this.isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName));
    }


    private void removeInjectCache(BeanFactory beanFactory, String beanName) {

        AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor =
                beanFactory.getBean(AutowiredAnnotationBeanPostProcessor.class);

        Map<String, InjectionMetadata> injectionMetadataMap =
                (Map<String, InjectionMetadata>) ReflectionUtils.getField(injectionMetadataCacheField, autowiredAnnotationBeanPostProcessor);

        injectionMetadataMap.remove(beanName.replace("scriptedObject", "scriptFactory"));
        injectionMetadataMap.remove(beanName);
    }

    private static Field injectionMetadataCacheField =
            ReflectionUtils.findField(AutowiredAnnotationBeanPostProcessor.class, "injectionMetadataCache");

    static {
        injectionMetadataCacheField.setAccessible(true);
    }
}
