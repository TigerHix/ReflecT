/**
 * The MIT License
 * Copyright (c) 2013 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.engine.reflect.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import de.cubeisland.engine.reflect.ReflectedTest;
import de.cubeisland.engine.reflect.ReflectedTest2;
import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.reflect.codec.yaml.YamlCodec;
import de.cubeisland.engine.reflect.exception.DuplicatedPathException;
import org.junit.Before;
import org.junit.Test;

import static de.cubeisland.engine.reflect.ReflectedFieldShadowing.ReflectedFieldShadowing2;
import static org.junit.Assert.assertEquals;


public class YamlReflectedTest
{
    private ReflectedTest test1;
    private ReflectedTest2 test2;
    private File file;

    private Reflector factory;
    private YamlCodec codec;

    @Before
    public void setUp() throws Exception
    {
        this.file = new File("../testReflected.yml");
        factory = new Reflector();
        test1 = factory.create(ReflectedTest.class);
        test2 = factory.create(ReflectedTest2.class);
        codec = factory.getCodecManager().getCodec(YamlCodec.class);
    }

    @Test
    public void test1() throws Exception
    {
        codec.saveReflected(test1, new FileOutputStream(file));
        final ReflectedTest reflected = factory.create(ReflectedTest.class);
        codec.loadReflected(reflected, new FileInputStream(file));
        file.delete();
        assertEquals(codec.convertReflected(test1).asString(), codec.convertReflected(reflected).asString());
    }

    @Test
    public void test2() throws Exception
    {
        codec.saveReflected(test2, new FileOutputStream(file));
        final ReflectedTest2 reflected = factory.create(ReflectedTest2.class);
        codec.loadReflected(reflected, new FileInputStream(file));
        file.delete();
        assertEquals(codec.convertReflected(test2).asString(), codec.convertReflected(reflected).asString());
    }

    @Test(expected = DuplicatedPathException.class)
    public void test3() throws Exception
    {
        factory.create(ReflectedFieldShadowing2.class);
    }
}
