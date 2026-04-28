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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.contrib.confluence.filter.internal.macros.MacroToContentConverter;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Listener;

/**
 * Converts span, bgcolor, highlight, tm and reg-tm macros into groups stylized to display inline.
 *
 * @version $Id$
 */
@Component(hints = { "span", "bgcolor", "highlight", "tm", "reg-tm" })
@Singleton
public class MacrosToSpanConverter extends MacroToContentConverter
{
    private static final String HTML_ATTRIBUTE_CLASS = "class";

    private static final String CONFLUENCE_OUTPUT_TYPE = "atlassian-macro-output-type";

    private static final String MACRO_BGCOLOR = "bgcolor";

    private static final String PARAMETER_COLOR = "color";

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>(confluenceParameters);

        // Add a class identifying the confluence macro name.
        String confluenceClass = String.format("confluence_%s_content", confluenceId);
        parameters.compute(HTML_ATTRIBUTE_CLASS,
            (k, v) -> v == null ? confluenceClass : String.join(" ", confluenceClass, v));

        // Add a class marking the group as inline.
        if (parameters.getOrDefault(CONFLUENCE_OUTPUT_TYPE, "BLOCK").equals("INLINE")) {
            // For some reason, even if bgcolor supports inline, the result renders as a block in confluence.
            parameters.compute(HTML_ATTRIBUTE_CLASS, (k, v) -> String.join(" ", "confluenceInline", v));
        }
        parameters.remove(CONFLUENCE_OUTPUT_TYPE);

        // Handle bgcolor and highlight macros that specify a background color.
        if (!parameters.getOrDefault(PARAMETER_COLOR, "").isEmpty()
            && (confluenceId.equals(MACRO_BGCOLOR) || confluenceId.equals("highlight")))
        {
            parameters.compute("style", (k, v) -> {
                String bgColorStyle = String.format("background-color: %s;", parameters.get(PARAMETER_COLOR));
                if (v == null) {
                    return bgColorStyle;
                } else {
                    return v.endsWith(";") ? String.format("%s;%s", v, bgColorStyle) : v.concat(bgColorStyle);
                }
            });
        }
        parameters.remove(PARAMETER_COLOR);

        return parameters;
    }

    @Override
    protected void endEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener) throws ConversionException
    {
        super.endEvent(id, parameters, content, inline, listener);
        // In case of the tm and reg-tm macros, the content needs to be superscripted by specific characters.
        if (id.equals("tm")) {
            listener.beginFormat(Format.SUPERSCRIPT, Collections.emptyMap());
            listener.onWord("TM");
            listener.endFormat(Format.SUPERSCRIPT, Collections.emptyMap());
        } else if (id.equals("reg-tm")) {
            listener.beginFormat(Format.SUPERSCRIPT, Collections.emptyMap());
            listener.onWord("®");
            listener.endFormat(Format.SUPERSCRIPT, Collections.emptyMap());
        }
    }
}
