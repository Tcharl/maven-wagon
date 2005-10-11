package org.apache.maven.wagon.providers.sshext;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.File;

/**
 * @author <a href="michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class TestData
{
    public static String getTestRepositoryUrl()
    {
        return "scp://beaver.codehaus.org//home/projects/" + getUserName() + "/public_html";
    }

    public static String getUserName()
    {

        String retValue = System.getProperty( "test.user" );

        if ( retValue == null )
        {
            retValue = System.getProperty( "user.name" );
        }

        return retValue;
    }

    public static File getPrivateKey()
    {
        File retValue = new File( System.getProperty( "user.home" ), "/.ssh/id_dsa" );

        return retValue;
    }
}
