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

import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.confluence.filter.internal.macros.MacroToContentConverter;
import org.xwiki.rendering.listener.Listener;

/**
 * Converts the table macros into XWiki table syntax.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(hints = { TableMacrosConverter.TABLE, TableMacrosConverter.THEAD, TableMacrosConverter.TR,
    TableMacrosConverter.TD, TableMacrosConverter.TBODY, TableMacrosConverter.TH })
@Singleton
public class TableMacrosConverter extends MacroToContentConverter
{
    /**
     * The name of the HTML table element.
     */
    public static final String TABLE = "table";

    /**
     * The name of the HTML thead element.
     */
    public static final String THEAD = "thead";

    /**
     * The name of the HTML tr element.
     */
    public static final String TR = "tr";

    /**
     * The name of the HTML td element.
     */
    public static final String TD = "td";

    /**
     * The name of the HTML tbody element.
     */
    public static final String TBODY = "tbody";

    /**
     * The name of the HTML th element.
     */
    public static final String TH = "th";

    @Override
    protected String toXWikiContent(String confluenceId, Map<String, String> parameters, String confluenceContent)
    {
        if (confluenceContent != null && confluenceId.equals(TABLE)) {
            return confluenceContent.replaceAll("(\\)\\)\\))\\s+(\\|\\(\\(\\()", "$1\n$2");
        }
        return confluenceContent;
    }

    @Override
    protected void beginEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener)
    {
        switch (id) {
            case TableMacrosConverter.TABLE:
                listener.beginTable(parameters);
                // Make sure the different tbody/thead/tr elements make a single table per "table" macro.
                break;
            case TableMacrosConverter.TR:
                listener.beginTableRow(parameters);
                break;
            case TableMacrosConverter.TH:
                listener.beginTableHeadCell(parameters);
                break;
            case TableMacrosConverter.TD:
                listener.beginTableCell(parameters);
                break;
            // In the case of thead and tbody macros, we simply render the content, as we don't have strong support
            // for them in the rendering listeners. This will lose the parameters.
            default:
        }
    }

    @Override
    protected void endEvent(String id, Map<String, String> parameters, String content, boolean inline,
        Listener listener)
    {
        switch (id) {
            case TableMacrosConverter.TABLE:
                listener.endTable(parameters);
                break;
            case TableMacrosConverter.TR:
                listener.endTableRow(parameters);
                break;
            case TableMacrosConverter.TH:
                listener.endTableHeadCell(parameters);
                break;
            case TableMacrosConverter.TD:
                listener.endTableCell(parameters);
                break;
            default:
        }
    }

    @Override
    protected Map<String, String> toXWikiParameters(String confluenceId, Map<String, String> confluenceParameters,
        String content)
    {
        return confluenceParameters;
    }
}
