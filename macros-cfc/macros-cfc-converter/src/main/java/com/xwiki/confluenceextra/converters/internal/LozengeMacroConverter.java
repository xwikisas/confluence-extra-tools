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

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.ConversionException;
import org.xwiki.contrib.confluence.filter.internal.input.ConfluenceConverter;
import org.xwiki.contrib.confluence.filter.internal.macros.MacroToContentConverter;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

/**
 * Converts the lozenge macro into a group wrapping the content and a link to the specified page.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("lozenge")
public class LozengeMacroConverter extends MacroToContentConverter
{
    private static final String HTML_ATTRIBUTE_STYLE = "style";

    @Inject
    private ConfluenceConverter converter;

    @Inject
    private ComponentManager componentManager;

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        Map<String, String> parameters = new HashMap<>(confluenceParameters);

        // Remove unhandled parameters.
        parameters.remove("arrow");
        parameters.remove("icon");
        // Convert the width/color parameter into a style parameter.
        String width = parameters.remove("width");
        if (width != null) {
            parameters.put(HTML_ATTRIBUTE_STYLE, String.format("width: %s;", width));
        }
        String color = parameters.remove("color");
        if (color != null) {
            String bgColorStyling = String.format("background-color: %s;", color);
            parameters.compute(HTML_ATTRIBUTE_STYLE,
                (k, v) -> v != null ? v.concat(bgColorStyling) : bgColorStyling);
        }
        return parameters;
    }

    @Override
    protected void beginEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener) throws ConversionException
    {
        // The link is either a page link or a external link.
        String link = parameters.remove("link");

        String title = parameters.remove("title");
        // Begin the group wrapping.
        super.beginEvent(id, parameters, content, inline, listener);
        // Send the events for the link/title.
        ResourceReference reference = null;
        if (link != null) {
            reference = converter.convertURL(link);
            listener.beginLink(reference, false, Collections.emptyMap());
        }
        // For some weird reason, the link parameter value in confluence syntax has this format <a href=link>link</a>
        // if it's an external link. In this case, the link won't be present in the parameters map and the events for
        // it will be present outside our group. We need to make sure we dont lose the title.
        if (title != null) {
            try {
                Parser plainTextParser = componentManager.getInstance(Parser.class, "plain/1.0");
                plainTextParser.parse(new StringReader(title)).getChildren().forEach(i -> i.traverse(listener));
            } catch (ComponentLookupException | ParseException ignored) {
            }
        }

        if (link != null) {
            listener.endLink(reference, false, Collections.emptyMap());
        }
    }
}
