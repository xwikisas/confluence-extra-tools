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

import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.AnyBlockMatcher;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * The Confluence wiki markup bridge macro.
 *
 * @version $Id$
 * @since 1.0.0
 */
@Component(hints = { "confluence_unmigrated-wiki-markup", "confluence_unmigrated-inline-wiki-markup" })
@Singleton
public class ConfluenceUnmigratedWikiMarkup extends AbstractMacro<ConfluenceUnmigratedWikiMarkupParameters>
{
    private static final String CONFLUENCE_1_1 = "confluence/1.1";

    private static final String CONFLUENCE_MACRO_PREFIX = "confluence_";

    /**
     * Used to find the Parser corresponding to the user-specified syntax for the Macro.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private MacroManager macroManager;

    /**
     * Constructor.
     */
    public ConfluenceUnmigratedWikiMarkup()
    {
        super("Confluence wiki markup", "Confluence bridge macro for wiki markup.",
            new DefaultContentDescriptor("Content", true, String.class),
            ConfluenceUnmigratedWikiMarkupParameters.class);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(ConfluenceUnmigratedWikiMarkupParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        try {
            Parser confluenceParser = getSyntaxParser();
            List<String> macroIds =
                macroManager.getMacroIds().stream().map(MacroId::getId).collect(Collectors.toList());
            XDOM xdom = confluenceParser.parse(new StringReader(content));
            removeUnknownMacroCall(xdom, macroIds, confluenceParser);
            return xdom.getChildren();
        } catch (ParseException e) {
            throw new MacroExecutionException(
                String.format("Failed to parse macro content in syntax [%s]", CONFLUENCE_1_1), e);
        } catch (MacroLookupException e) {
            throw new MacroExecutionException("Failed to lockup for available macros", e);
        }
    }

    /**
     * Get the parser for the passed Syntax.
     *
     * @return the matching Parser that can be used to parse content in the passed Syntax
     * @throws MacroExecutionException if there's no Parser in the system for the passed Syntax
     */
    private Parser getSyntaxParser() throws MacroExecutionException
    {
        ComponentManager componentManager = this.componentManagerProvider.get();
        if (componentManager.hasComponent(Parser.class, CONFLUENCE_1_1)) {
            try {
                return componentManager.getInstance(Parser.class, CONFLUENCE_1_1);
            } catch (ComponentLookupException e) {
                throw new MacroExecutionException(
                    String.format("Failed to lookup Parser for syntax [%s]", CONFLUENCE_1_1), e);
            }
        } else {
            throw new MacroExecutionException(
                String.format("Cannot find Parser for syntax [%s]", CONFLUENCE_1_1));
        }
    }

    /**
     * In the wiki markup syntax we can find some macro. Sometime this macro could be implemented in XWiki and it might
     * work just because the macro is found in XWiki. But they are a height risk that the macro is not implemented in
     * XWiki and so the rendering will crash just because the macro is not found. This method is intended to reduce the
     * consequence of this by: - Trying to also find a confluence bridge - If not implementation is found just replace
     * the macro call by the content so instead of showing an error we just show the macro content, which is better than
     * nothing even if it's not ideal. Note this is also the behavior in confluence, in this case. There are a warning
     * about the macro not found but the macro content are still rendered.
     *
     * @param xdom root element to handle
     * @param macroIds list of the current available macro ids in the XWiki instance
     * @param confluenceParser the confluence parser. Used to avoid to call getSyntaxParser at each parsing.
     * @throws ParseException in case the parsing fail.
     */
    private void removeUnknownMacroCall(XDOM xdom, List<String> macroIds, Parser confluenceParser)
        throws ParseException
    {
        for (Block block : xdom.getBlocks(AnyBlockMatcher.ANYBLOCKMATCHER, Block.Axes.DESCENDANT)) {
            if (block instanceof MacroBlock) {
                String macroId = ((MacroBlock) block).getId();
                boolean macroFound = false;
                if (macroIds.contains(macroId)) {
                    macroFound = true;
                } else if (macroIds.contains(CONFLUENCE_MACRO_PREFIX + macroId)) {
                    // we found a bridge, rename it
                    MacroBlock newMacro = new MacroBlock(CONFLUENCE_MACRO_PREFIX + macroId, block.getParameters(),
                        ((MacroBlock) block).getContent(), ((MacroBlock) block).isInline());
                    block.getParent().replaceChild(newMacro, block);
                    macroFound = true;
                }
                if (!macroFound) {
                    // No macro implementation found, just show the content
                    XDOM xd = confluenceParser.parse(new StringReader(((MacroBlock) block).getContent()));
                    removeUnknownMacroCall(xd, macroIds, confluenceParser);
                    block.getParent().replaceChild(xd.getChildren(), block);
                }
            }
        }
    }
}
