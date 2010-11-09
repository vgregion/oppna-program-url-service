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

package se.vgregion.urlservice.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.vgregion.urlservice.repository.RedirectRuleRepository;
import se.vgregion.urlservice.repository.ShortLinkRepository;
import se.vgregion.urlservice.repository.StaticRedirectRepository;
import se.vgregion.urlservice.types.RedirectRule;
import se.vgregion.urlservice.types.ShortLink;
import se.vgregion.urlservice.types.StaticRedirect;

@Service
public class DefaultUrlServiceService implements UrlServiceService {

    private final Logger log = LoggerFactory.getLogger(DefaultUrlServiceService.class);
    
    private static final List<String> WHITELISTED_SCHEMES = Arrays.asList(new String[] {"http", "https"});

    private static final int INITIAL_HASH_LENGTH = 6;
    
    private String urlPrefix;
    
    private ShortLinkRepository shortLinkRepository;
    private RedirectRuleRepository redirectRuleRepository;
    private StaticRedirectRepository staticRedirectRepository;
    
    public DefaultUrlServiceService() {
        log.info("Created {}", DefaultUrlServiceService.class.getName());
    }

    
    /** 
     * {@inheritDoc}
     */
    @Transactional(readOnly = false)
    public ShortLink shorten(String urlString) throws URISyntaxException {
        return shorten(urlString, null);
    }
    /** 
     * {@inheritDoc}
     */
    @Transactional(readOnly = false)
    public ShortLink shorten(String urlString, String hash) throws URISyntaxException {
        URI url = new URI(urlString);
        
        if(WHITELISTED_SCHEMES.contains(url.getScheme())) {
            ShortLink link = shortLinkRepository.findByLongUrl(urlString);
            
            if(link != null) {
                return link;
            } else {
                String md5 = DigestUtils.md5Hex(urlString);
                
                int length = INITIAL_HASH_LENGTH;
                
                if(StringUtils.isBlank(hash)) {
                    hash = md5.substring(0, length);
                }
                // check that the hash does not already exist
                while(shortLinkRepository.findByHash(hash) != null) {
                    length++;
                    
                    if(length > md5.length()) {
                        // should never happen...
                        throw new RuntimeException("Failed to generate hash");
                    }
                    
                    hash += md5.substring(length-1, length);
                }
                
                ShortLink newLink = new ShortLink();
                newLink.setHash(hash);
                newLink.setLongUrl(urlString);
                newLink.setShortUrl(urlPrefix + hash);
                
                shortLinkRepository.persist(newLink);
                return newLink;
            }
        } else {
            throw new URISyntaxException(urlString, "Scheme not allowed");
        }
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ShortLink expand(String shortUrlOrHash) {
        String hash;
        if(shortUrlOrHash.startsWith("http://")) {
            hash = shortUrlOrHash.substring(shortUrlOrHash.lastIndexOf('/') + 1);
        } else {
            hash = shortUrlOrHash;
        }
        return shortLinkRepository.findByHash(hash);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public URI redirect(String path) {
        ShortLink shortLink = expand(path);
        
        // first try short links
        if(shortLink != null) {
            return URI.create(shortLink.getLongUrl());
        } else {
            // next, try static redirects
            StaticRedirect redirect = staticRedirectRepository.findByPath(path);
            
            if(redirect != null) {
                return URI.create(redirect.getUrl());
            } else {
                // finally, check redirect rules
                Collection<RedirectRule> rules = redirectRuleRepository.findAll();
                
                for(RedirectRule rule : rules) {
                    if(rule.matches(path)) {
                        return URI.create(rule.getUrl());
                    }
                }
                
                return null;
            }
        }
    }

    
    /** 
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ShortLink lookup(String url) throws URISyntaxException {
        return shortLinkRepository.findByLongUrl(url);
    }

    public ShortLinkRepository getShortLinkRepository() {
        return shortLinkRepository;
    }

    @Resource
    public void setShortLinkRepository(ShortLinkRepository shortLinkRepository) {
        this.shortLinkRepository = shortLinkRepository;
    }
    
    public RedirectRuleRepository getRedirectRuleRepository() {
        return redirectRuleRepository;
    }

    @Resource
    public void setRedirectRuleRepository(RedirectRuleRepository redirectRuleRepository) {
        this.redirectRuleRepository = redirectRuleRepository;
    }

    public StaticRedirectRepository getStaticRedirectRepository() {
        return staticRedirectRepository;
    }

    @Resource
    public void setStaticRedirectRepository(StaticRedirectRepository staticRedirectRepository) {
        this.staticRedirectRepository = staticRedirectRepository;
    }


    public String getUrlPrefix() {
        return urlPrefix;
    }

    @Resource(name="urlPrefix")
    public void setUrlPrefix(String urlPrefix) {
        if(!urlPrefix.endsWith("/")) {
            urlPrefix = urlPrefix + "/";
        }
        
        this.urlPrefix = urlPrefix;
    }



}