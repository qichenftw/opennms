package org.opennms.web.controller.inventory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.opennms.web.springframework.security.Authentication;
import org.opennms.web.svclayer.inventory.InventoryService;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;


/**
 * <p>AdminRancidDeleteController class.</p>
 *
 * @author ranger
 * @version $Id: $
 * @since 1.8.1
 */
public class AdminRancidDeleteController extends SimpleFormController {

    InventoryService m_inventoryService;
        
    /**
     * <p>getInventoryService</p>
     *
     * @return a {@link org.opennms.web.svclayer.inventory.InventoryService} object.
     */
    public InventoryService getInventoryService() {
        return m_inventoryService;
    }

    /**
     * <p>setInventoryService</p>
     *
     * @param inventoryService a {@link org.opennms.web.svclayer.inventory.InventoryService} object.
     */
    public void setInventoryService(InventoryService inventoryService) {
        m_inventoryService = inventoryService;
    }

    /** {@inheritDoc} */
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors) throws ServletException, IOException, Exception {

        log().debug("AdminRancidDeleteController ModelAndView onSubmit");

        AdminRancidRouterDbCommClass bean = (AdminRancidRouterDbCommClass) command;
                       
        log().debug("AdminRancidDeleteController ModelAndView onSubmit delete device["+ bean.getDeviceName() + "] group[" + bean.getGroupName() + "] status[" + bean.getStatusName()+"]");

        if (request.isUserInRole(Authentication.ROLE_ADMIN)) {

        boolean done = m_inventoryService.deleteNodeOnRouterDb(bean.getGroupName(), bean.getDeviceName());
        if (!done){
            log().debug("AdminRancidDeleteController ModelAndView onSubmit error while deleting status for"+ bean.getGroupName() + "/" + bean.getDeviceName());
        }
        }
        String redirectURL = request.getHeader("Referer");
        response.sendRedirect(redirectURL);
        return super.onSubmit(request, response, command, errors);
    }

    /** {@inheritDoc} */
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder)
        throws ServletException {
        log().debug("AdminRancidStatusController initBinder");
    }
    
    private static Logger log() {
        return Logger.getLogger("Rancid");
    }
}
