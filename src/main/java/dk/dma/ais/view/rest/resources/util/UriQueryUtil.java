/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.ais.view.rest.resources.util;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * 
 * @author Kasper Nielsen
 */
public class UriQueryUtil {

    public static List<Integer> getParametersAsInt(UriInfo info, String name) {
        return convertToInt(getParameters(info, name), name);
    }

    public static List<String> getParameters(UriInfo info, String name) {
        List<String> list = info.getQueryParameters().get(requireNonNull(name));
        return list == null ? Collections.<String> emptyList() : list;
    }

    public static String getOneOrZeroParametersOrFail(UriInfo info, String name) {
        return getOneOrZeroParametersOrFail(info, name, null);
    }

    public static String getOneOrZeroParametersOrFail(UriInfo info, String name, String defaultValue) {
        List<String> p = getParameters(info, name);
        if (p.size() > 1) {
            throw new WebApplicationException();
        }
        return p.isEmpty() ? defaultValue : p.get(0);
    }

    static List<Integer> convertToInt(List<String> params, String parameterName) {
        List<Integer> result = new ArrayList<>();
        for (String s : params) {
            try {
                result.add(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                throw new WebApplicationException("Expected a valid integer for parameter '" + parameterName
                        + "' but was " + parameterName + " = " + s, Response.Status.BAD_REQUEST);
            }
        }
        return result;
    }
}
