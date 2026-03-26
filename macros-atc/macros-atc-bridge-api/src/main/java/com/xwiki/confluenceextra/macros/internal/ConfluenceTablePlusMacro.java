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
package com.xwiki.confluenceextra.macros.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.date.DateMacroConfiguration;

/**
 * Confluence Macro bridge for table-plus.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(ConfluenceTablePlusMacro.ID)
public class ConfluenceTablePlusMacro extends AbstractMacro<ConfluenceTablePlusMacroParameters>
{
    static final String ID = "confluence_table-plus";

    private static final String LIVEDATA_INLINE_TABLE_ID = "livedata-inline-table";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private TransformationManager transformationManager;

    @Inject
    private DateMacroConfiguration dateMacroConfiguration;

    /**
     * Constructor.
     */
    public ConfluenceTablePlusMacro()
    {
        super("Confluence Table Plus Macro Bridge", "Bridge macro for Confluence Table Plus.",
            new DefaultContentDescriptor("Content", true, Block.LIST_BLOCK_TYPE),
            ConfluenceTablePlusMacroParameters.class);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(ConfluenceTablePlusMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        Map<String, String> newParameters = new HashMap<>(context.getCurrentMacroBlock().getParameters());

        // Set the dateFormats.

        List<String> dateFormats = new ArrayList<>();

        // Date macro's configured format.
        dateFormats.add(this.dateMacroConfiguration.getDisplayDateFormat());

        // XWiki's default format.
        XWikiContext xcontext = contextProvider.get();
        dateFormats.add(xcontext.getWiki().getXWikiPreference("dateformat", "yyyy/MM/dd HH:mm", xcontext));
        newParameters.put("dateFormats", String.join("||", dateFormats.toArray(new String[0])));

        // This macro is simply an alias.
        Block block = new GroupBlock(Collections
            .singletonList(new MacroBlock(LIVEDATA_INLINE_TABLE_ID, newParameters, content, context.isInline())));

        // Render the livedata-inline-table macro.
        try {
            transformationManager.performTransformations(block, context.getTransformationContext());
        } catch (TransformationException e) {
            throw new MacroExecutionException(e.getMessage(), e);
        }

        // Strip the MacroMarkerBlock in order to have an alias rather than nested macros.
        return block.getChildren().get(0).getChildren();
    }
}
