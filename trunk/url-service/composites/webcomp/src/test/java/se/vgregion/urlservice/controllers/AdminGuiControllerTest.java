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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import se.vgregion.urlservice.repository.RedirectRuleRepository;
import se.vgregion.urlservice.types.RedirectRule;


public class AdminGuiControllerTest {

    private static final String PATTERN = "foo";
    private static final String URL = "http://google.com";
    private AdminGuiController controller = new AdminGuiController();
    
    @Test
    public void index() throws IOException {
        List<RedirectRule> rules = Arrays.asList(new RedirectRule(PATTERN, URL));
        
        RedirectRuleRepository redirectRuleRepository = mock(RedirectRuleRepository.class);
        when(redirectRuleRepository.findAll()).thenReturn(rules);
        controller.setRedirectRuleRepository(redirectRuleRepository);
        
        ModelAndView mav = controller.index();
        
        Assert.assertEquals(rules, mav.getModel().get("redirectRules"));
    }
    
    @Test
    public void addRedirectRule() throws IOException {
        RedirectRuleRepository redirectRuleRepository = mock(RedirectRuleRepository.class);
        controller.setRedirectRuleRepository(redirectRuleRepository);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("add", "any value");
        request.addParameter("pattern", PATTERN);
        request.addParameter("url", URL);
        
        controller.update(request);

        verify(redirectRuleRepository).persist(new RedirectRule(PATTERN, URL));
    }

    @Test
    public void deleteRedirectRule() throws IOException {
        RedirectRuleRepository redirectRuleRepository = mock(RedirectRuleRepository.class);
        controller.setRedirectRuleRepository(redirectRuleRepository);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("delete-1", "any value");
        
        controller.update(request);

        verify(redirectRuleRepository).removeByPrimaryKey(1L);
    }

}
