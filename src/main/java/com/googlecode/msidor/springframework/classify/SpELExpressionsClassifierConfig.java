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


/**
 * Single "expression to value" mapping of a {@link SpELExpressionsClassifier}.
 * 
 * @author Maciej SIDOR (maciejsidor@gmail.com)
 * @since 2015
 */
public class SpELExpressionsClassifierConfig <T>
{

	/**
	 * SpEL Expression must evaluate to true or false.
	 */
	private String expression;
	
	/**
	 * Value to be returned if SpEL Expression evaluates to true
	 */
	private T value;
	
	/**
	 * If set to true, and classifier finds no satisfying mapping, the assigned value will be returned
	 */
	private boolean isDefault = false;
	
	/**
	 * SpEL Expression must evaluate to true or false.
	 * @return the expression
	 */
	public String getExpression() 
	{
		return expression;
	}
	
	/**
	 * Sets SpEL Expression. 
	 * Expression must evaluate to true or false.
	 * 
	 * @param expression the expression to set
	 */
	public void setExpression(String expression) 
	{
		this.expression = expression;
	}
	
	/**
	 * Value to be returned if SpEL Expression evaluates to true
	 * @return the value
	 */
	public T getValue() 
	{
		return value;
	}
	
	/**
	 * Sets value that will be returned if the SpEL expressions evaluates to true
	 * @param value the value to set
	 */
	public void setValue(T value) 
	{
		this.value = value;
	}
	
	/**
	 * If set to true, and classifier finds no satisfying mapping, the assigned value will be returned
	 * @return the isDefault
	 */
	public boolean isDefault() 
	{
		return isDefault;
	}
	
	/**
	 * If set to true, and classifier finds no satisfying mapping, the assigned value will be returned
	 * @param isDefault the isDefault to set
	 */
	public void setDefault(boolean isDefault) 
	{
		this.isDefault = isDefault;
	}

}
