/*
Copyright 2015 Maciej SIDOR [maciejsidor@gmail.com]

The source code is licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.	
 */
package com.googlecode.msidor.springframework.classify;

import java.util.ArrayList;
import java.util.List;

import org.springframework.classify.Classifier;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * A {@link Classifier} that evaluates input Object by SpEL expression and, based on evaluation result maps to value of a given type.
 * SpEL Expression must evaluate to true or false. 
 * Single "expression to value" mapping is configured by {@link SpELExpressionsClassifierConfig}.
 * Mappings are order sensitives with means that first expression that evaluates to true on input objects returns the value. 
 * 
 * @author Maciej SIDOR (maciejsidor@gmail.com)
 * @since 2015
 */
public class SpELExpressionsClassifier<K, T> implements Classifier<K, T> 
{

	/**
	 * SpEL parser
	 */
	private ExpressionParser parser = new SpelExpressionParser();

	/**
	 * Expressions and assigned values
	 */
	private List<SpELExpressionsClassifierConfig<T>> values = null;

	/**
	 * Default constructor. Use the setter or the other constructor to create a
	 * sensible classifier, otherwise all inputs will cause an exception.
	 */
	public SpELExpressionsClassifier() 
	{
		this(new ArrayList<SpELExpressionsClassifierConfig<T>>());
	}

	/**
	 * Create a classifier from the list of "SpEL expression to value" mappings.
	 * See {@link SpELExpressionsClassifierConfig} for more info.
	 * 
	 * @param values List of {@link SpELExpressionsClassifierConfig}
	 */
	public SpELExpressionsClassifier(List<SpELExpressionsClassifierConfig<T>> values) 
	{
		super();
		this.values = values;
	}
	
	/**
	 * Set list of "SpEL expression to value" mappings
	 * @param values List of {@link SpELExpressionsClassifierConfig}
	 */
	public void setValues(List<SpELExpressionsClassifierConfig<T>> values) 
	{
		this.values = values;
	}

	/**
	 * Classify the input object by evaluating it on SpEL expression and, based on evaluation result maps to value of a given type.
	 * SpEL Expression must evaluate to true or false. 
	 * First expression that evaluates to true on input objects returns the value.
	 * 
	 * @return the value for which the SpEL expression evaluates to true on input object.
	 */
	public T classify(K classifiable) 
	{
		if(values!=null && values.size()>0)
		{
			T defaultValue = null;
			
			for (SpELExpressionsClassifierConfig<T> config : values) 
			{							
				Expression exp = parser.parseExpression(config.getExpression() );
				boolean result = exp.getValue(classifiable, Boolean.class);
				
				if(result)
				{
					return config.getValue();
				}
				
				if(config.isDefault())
				{
					defaultValue = config.getValue();
				}
			}
			
			
			return defaultValue;

		}
		
		return null;
	}


}
