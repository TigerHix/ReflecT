/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Anselm Brehme, Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.cubeisland.engine.configuration.codec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.cubeisland.engine.configuration.convert.Converter;
import de.cubeisland.engine.configuration.convert.converter.BooleanConverter;
import de.cubeisland.engine.configuration.convert.converter.ByteConverter;
import de.cubeisland.engine.configuration.convert.converter.DateConverter;
import de.cubeisland.engine.configuration.convert.converter.DoubleConverter;
import de.cubeisland.engine.configuration.convert.converter.FloatConverter;
import de.cubeisland.engine.configuration.convert.converter.IntegerConverter;
import de.cubeisland.engine.configuration.convert.converter.LocaleConverter;
import de.cubeisland.engine.configuration.convert.converter.LongConverter;
import de.cubeisland.engine.configuration.convert.converter.ShortConverter;
import de.cubeisland.engine.configuration.convert.converter.StringConverter;
import de.cubeisland.engine.configuration.convert.converter.UUIDConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.ArrayConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.CollectionConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.exception.ConversionException;
import de.cubeisland.engine.configuration.exception.ConverterNotFoundException;
import de.cubeisland.engine.configuration.node.ListNode;
import de.cubeisland.engine.configuration.node.MapNode;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.NullNode;

public final class ConverterManager
{
    private Map<Class, Converter> converters = new ConcurrentHashMap<Class, Converter>();
    private MapConverter mapConverter;
    private ArrayConverter arrayConverter;
    private CollectionConverter collectionConverter;
    private ConverterManager defaultConverters;

    private ConverterManager(ConverterManager defaultConverters)
    {
        this.defaultConverters = defaultConverters;
        this.mapConverter = new MapConverter();
        this.arrayConverter = new ArrayConverter();
        this.collectionConverter = new CollectionConverter();
    }

    static ConverterManager defaultManager()
    {
        // Register Default Converters
        ConverterManager convert = new ConverterManager(null);
        convert.registerDefaultConverters();
        return convert;
    }

    static ConverterManager emptyManager(ConverterManager defaultConverters)
    {
        return new ConverterManager(defaultConverters);
    }

    private void registerDefaultConverters()
    {
        Converter<?> converter;
        this.registerConverter(Integer.class, converter = new IntegerConverter());
        this.registerConverter(int.class, converter);
        this.registerConverter(Short.class, converter = new ShortConverter());
        this.registerConverter(short.class, converter);
        this.registerConverter(Byte.class, converter = new ByteConverter());
        this.registerConverter(byte.class, converter);
        this.registerConverter(Double.class, converter = new DoubleConverter());
        this.registerConverter(double.class, converter);
        this.registerConverter(Float.class, converter = new FloatConverter());
        this.registerConverter(float.class, converter);
        this.registerConverter(Long.class, converter = new LongConverter());
        this.registerConverter(long.class, converter);
        this.registerConverter(Boolean.class, converter = new BooleanConverter());
        this.registerConverter(boolean.class, converter);
        this.registerConverter(String.class, new StringConverter());
        this.registerConverter(Date.class, new DateConverter());
        this.registerConverter(UUID.class, new UUIDConverter());
        this.registerConverter(Locale.class, new LocaleConverter());
    }

    /**
     * registers a converter to check for when converting
     *
     * @param clazz     the class
     * @param converter the converter
     */
    public final void registerConverter(Class clazz, Converter converter)
    {
        if (clazz == null || converter == null)
        {
            return;
        }
        converters.put(clazz, converter);
    }

    /**
     * Removes a converter from this manager
     *
     * @param clazz the class of the converter to remove
     */
    public final void removeConverter(Class clazz)
    {
        Iterator<Map.Entry<Class, Converter>> iter = converters.entrySet().iterator();
        Map.Entry<Class, Converter> entry;
        while (iter.hasNext())
        {
            entry = iter.next();
            if (entry.getKey() == clazz || entry.getValue().getClass() == clazz)
            {
                iter.remove();
            }
        }
    }

    /**
     * Removes all registered converters
     */
    public final void removeConverters()
    {
        converters.clear();
    }

    /**
     * Searches matching Converter
     *
     * @param objectClass the class to search for
     *
     * @return a matching converter or null if not found
     */
    @SuppressWarnings("unchecked")
    public final <T> Converter<T> matchConverter(Class<? extends T> objectClass) throws ConverterNotFoundException
    {
        if (objectClass == null)
        {
            return null;
        }
        Converter converter = converters.get(objectClass);
        if (converter == null)
        {
            for (Map.Entry<Class, Converter> entry : converters.entrySet())
            {
                if (entry.getKey().isAssignableFrom(objectClass))
                {
                    registerConverter(objectClass, converter = entry.getValue());
                    break;
                }
            }
        }
        if (converter != null)
        {
            return (Converter<T>)converter;
        }
        if (objectClass.isArray() || Collection.class.isAssignableFrom(objectClass)
         || Map.class.isAssignableFrom(objectClass))
        {
            return null;
        }
        throw new ConverterNotFoundException("Converter not found for: " + objectClass.getName());
    }


    /**
     * Converts a convertible Object into a Node
     *
     * @param object the Object
     *
     * @return the serialized Node
     */
    public final <T> Node convertToNode(T object) throws ConversionException
    {
        try
        {
            return this.convertToNode0(object);
        }
        catch (ConverterNotFoundException e)
        {
            if (this.defaultConverters == null)
            {
                throw e;
            }
            return this.defaultConverters.convertToNode(object);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Node convertToNode0(T object) throws ConversionException
    {
        if (object == null)
        {
            return NullNode.emptyNode();
        }
        if (object.getClass().isArray())
        {
            return arrayConverter.toNode(this, (Object[])object);
        }
        else if (object instanceof Collection)
        {
            return collectionConverter.toNode(this, (Collection)object);
        }
        else if (object instanceof Map)
        {
            return mapConverter.toNode(this, (Map)object);
        }
        Converter<T> converter = (Converter<T>)matchConverter(object.getClass());
        return converter.toNode(this, object);
    }

    /**
     * Converts a Node back into the original Object
     *
     * @param node the node
     * @param type the type of the object
     *
     * @return the original object
     */
    public final <T> T convertFromNode(Node node, Type type) throws ConversionException
    {
        try
        {
            return this.convertFromNode0(node, type);
        }
        catch (ConverterNotFoundException e)
        {
            if (this.defaultConverters == null)
            {
                throw e;
            } // else ignore
            return this.defaultConverters.convertFromNode0(node, type);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertFromNode0(Node node, Type type) throws ConversionException
    {
        if (node == null || node instanceof NullNode || type == null)
        {
            return null;
        }
        if (type instanceof Class)
        {
            if (((Class)type).isArray())
            {
                if (node instanceof ListNode)
                {
                    return (T)arrayConverter.fromNode(this, (Class<T[]>)type, (ListNode)node);
                }
                else
                {
                    throw ConversionException.of(arrayConverter, node, "Cannot convert to Array! Node is not a ListNode!");
                }
            }
            else
            {
                Converter<T> converter = matchConverter((Class<T>)type);
                return converter.fromNode(this, node);
            }
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType ptype = (ParameterizedType)type;
            if (ptype.getRawType() instanceof Class)
            {
                if (Collection.class.isAssignableFrom((Class)ptype.getRawType()))
                {
                    if (node instanceof ListNode)
                    {
                        return (T)collectionConverter.<Object, Collection<Object>>fromNode(this, ptype, (ListNode)node);
                    }
                    else
                    {
                        throw ConversionException.of(collectionConverter, node, "Cannot convert to Collection! Node is not a ListNode!");
                    }
                }
                else if (Map.class.isAssignableFrom((Class)ptype.getRawType()))
                {
                    if (node instanceof MapNode)
                    {
                        return (T)mapConverter.<Object, Object, Map<Object, Object>>fromNode(this, ptype, (MapNode)node);
                    }
                    else
                    {
                        throw ConversionException.of(mapConverter, node, "Cannot convert to Map! Node is not a MapNode!");
                    }
                }
            }
        }
        throw new IllegalArgumentException("Unknown Type: " + type);
    }
}
