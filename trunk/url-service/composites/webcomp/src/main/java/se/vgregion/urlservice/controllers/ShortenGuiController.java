/**
 * Copyright 2010 Västra Götalandsregionen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public
 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *   Boston, MA 02111-1307  USA
 *
 */

package se.vgregion.urlservice.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import se.vgregion.urlservice.services.UrlServiceService;
import se.vgregion.urlservice.types.Keyword;
import se.vgregion.urlservice.types.ShortLink;

/**
 * Controller for showing a basic web GUI for shorting link.
 *
 */
@Controller
public class ShortenGuiController {

    private final Logger log = LoggerFactory.getLogger(ShortenGuiController.class);

    @Resource(name="domain")
    private String domain;
    
    @Resource
    private UrlServiceService urlServiceService;

    public ShortenGuiController() {
        log.info("Created {}", ShortenGuiController.class.getName());
    }

    public ShortenGuiController(UrlServiceService urlServiceService) {
        this();
        this.urlServiceService = urlServiceService;
    }

    @RequestMapping(value="/shorten")
    public ModelAndView index(@RequestParam(value="longurl", required=false) String longUrl, @RequestParam(value="slug", required=false) String slug, @RequestParam(value="keywords", required=false) List<UUID> keywordIds) throws IOException {
        ModelAndView mav = new ModelAndView("shorten");
        
        String userName = null;
        if(SecurityContextHolder.getContext().getAuthentication() != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(principal instanceof User) {
                userName = ((User) principal).getUsername();
                mav.addObject("authenticated", true);
                mav.addObject("userid", userName);
            }
        }
        
        mav.addObject("domain", domain);
        mav.addObject("keywords", urlServiceService.getAllKeywords());
        if(longUrl != null) {
            mav.addObject("longUrl", longUrl);
            try {
                ShortLink shortLink;
                if(userName != null) {
                    se.vgregion.urlservice.types.User user = urlServiceService.getUser(userName);
                    
                    shortLink = urlServiceService.shorten(longUrl, slug, keywordIds, user);
                } else {
                    shortLink = urlServiceService.shorten(longUrl, slug, keywordIds, null);
                }

                mav.addObject("shortUrl", shortLink.getShortUrl());
                mav.addObject("owned", shortLink.getOwner() != null && shortLink.getOwner().getVgrId().equals(userName));
                List<UUID> storedKeywordIds = new ArrayList<UUID>();
                if(shortLink.getKeywords() != null) {
                    for(Keyword keyword : shortLink.getKeywords()) {
                        storedKeywordIds.add(keyword.getId());
                    }
                }
                mav.addObject("keywordIds", storedKeywordIds);
                mav.addObject("slug", slug);
            } catch (URISyntaxException e) {
                mav.addObject("error", "Felaktig address, måste börja med \"http://\" eller \"https://\"");
            }
        } else {
            mav.addObject("owned", true);
        }
        
        return mav;
    }
}
