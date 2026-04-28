/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.confluenceextra.converters.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.macros.MacroToContentConverter;

/**
 * Converts align, div, p to groups.
 *
 * @version $Id$.
 */
@Component(hints = { "align", "div", "p" })
@Singleton
public class MacrosToGroupConverter extends MacroToContentConverter
{
    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>(confluenceParameters);
        String confluenceClass = String.format("confluence_%s_content", confluenceId);
        parameters.compute("class",
            (k, v) -> v == null ? confluenceClass : String.join(" ", confluenceClass, v));

        if (confluenceId.equals("center")) {
            parameters.put("style", "text-align: center");
        }

        return parameters;
    }
}
