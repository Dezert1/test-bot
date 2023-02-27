package com.bottest.scripts.altrecbots.utils;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * Класс ReflectionUtils предназначен для выполнения операций с рефлексией.
 */
public class ReflectionUtils
{
    /**
     * Метод forEachField позволяет перебрать все поля объекта, заданного аргументом field, с помощью передаваемого предиката mutableField, который принимает каждое поле и соответствующий ему объект Mutable.
     * Предикат mutableField возвращает true, чтобы продолжить перебор, или false, чтобы прервать его. Причем он получает все поля, не только public, но и protected, private.
     * @param field
     * @param mutableField
     * @param <T>
     * @param <V>
     * @return
     * @throws Exception
     */
	public static <T, V> boolean forEachField(T field, BiPredicate<Field, Mutable<V>> mutableField) throws Exception
	{
		synchronized(field)
		{
			return AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
			{
				Class<?> class_ = field.getClass();
				for(Field f : FieldUtils.getAllFields(class_))
				{
					boolean accessible = f.isAccessible();
					try
					{
						if(mutableField.test(f, new Mutable<V>()
						{
                            @Override
							public V getValue()
							{
								try
								{
									if(!accessible)
									{
										f.setAccessible(true);
									}

									return (V) f.get(field);
								}
								catch(Exception e)
								{
									throw new RuntimeException(e);
								}
							}

							@Override
							public void setValue(V value)
							{
								try
								{
									if(!accessible)
									{
										f.setAccessible(true);
									}
									f.set(field, value);
								}
								catch(Exception e)
								{
									throw new RuntimeException(e);
								}
							}
						}))
						return false;
					}
					finally
					{
						if(!accessible && f.isAccessible())
						{
							f.setAccessible(false);
						}
					}
				}
				return true;
			});
		}
	}
}