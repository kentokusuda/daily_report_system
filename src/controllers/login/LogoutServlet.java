package controllers.login;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.Employee;
import models.Report;
import utils.DBUtil;

/**
 * Servlet implementation class LogoutServlet
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public LogoutServlet() {
        super();
    }


    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //ここから

        EntityManager em = DBUtil.createEntityManager();
        //退勤時刻処理
        Employee e = (Employee)request.getSession().getAttribute("login_employee");

        LocalDate ldt = LocalDate.now();
        Date report_date = Date.valueOf(ldt);

        //↓SELECT r FROM Report AS r WHERE r.employee = :employee AND r.report_date = :report_date
        Report r = em.createNamedQuery("getLatestReport" , Report.class)
                .setParameter("employee", e).setParameter("report_date", report_date).getSingleResult();


        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        r.setLogout_time(currentTime);


        em.getTransaction().begin();
        em.getTransaction().commit();
        em.close();

        //ここまで


        request.getSession().removeAttribute("login_employee");

        request.getSession().setAttribute("flush", "ログアウトしました。");
        response.sendRedirect(request.getContextPath() + "/login");
    }

}