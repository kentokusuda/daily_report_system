package controllers.login;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.Employee;
import models.Report;
import utils.DBUtil;
import utils.EncryptUtil;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoginServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    // ログイン画面を表示
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("_token", request.getSession().getId());
        request.setAttribute("hasError", false);
        if (request.getSession().getAttribute("flush") != null) {
            request.setAttribute("flush", request.getSession().getAttribute("flush"));
            request.getSession().removeAttribute("flush");
        }

        RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/views/login/login.jsp");
        rd.forward(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    // ログイン処理
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 認証結果を格納する変数
        Boolean check_result = false;

        String code = request.getParameter("code");
        String plain_pass = request.getParameter("password");

        Employee e = null;
        EntityManager em = DBUtil.createEntityManager();
        if (code != null && !code.equals("") && plain_pass != null && !plain_pass.equals("")) {

            String password = EncryptUtil.getPasswordEncrypt(
                    plain_pass,
                    (String) this.getServletContext().getAttribute("salt"));

            // 社員番号とパスワードが正しいかチェック
            try {
                e = em.createNamedQuery("checkLoginCodeAndPassword", Employee.class)
                        .setParameter("code", code)
                        .setParameter("pass", password)
                        .getSingleResult();
            } catch (NoResultException ex) {
            }

            if (e != null) {
                check_result = true;
            }
        }

        if (!check_result) {
            // 認証できなかったらログイン画面に戻る
            em.close();
            request.setAttribute("_token", request.getSession().getId());
            request.setAttribute("hasError", true);
            request.setAttribute("code", code);

            RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/views/login/login.jsp");
            rd.forward(request, response);
        } else {

            // 認証できたらログイン状態にする
            request.getSession().setAttribute("login_employee", e);

            LocalDate ldt = LocalDate.now();
            Date report_date = Date.valueOf(ldt);

            long check_report = (long) em.createNamedQuery("checkMyReportCount", Long.class)
                    .setParameter("employee", e).setParameter("report_date", report_date).getSingleResult();

            if (check_report == 0) {

                //日付とログイン時間(出勤時間)のみはいった日報を作成する
                Report r = new Report();

                //人
                r.setEmployee((Employee) e);

                //日付
                r.setReport_date(report_date);

                //この2つはあとで消す
                r.setTitle("タイトルテストです");
                r.setContent("内容テストです");

                //時間ふたつ
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                r.setCreated_at(currentTime);//ログイン時間にする
                r.setUpdated_at(currentTime);

                em.getTransaction().begin();
                em.persist(r);
                em.getTransaction().commit();
                em.close();

            } else {
                em.close();
            }
        }

        //topへリダイレクト

        request.getSession().setAttribute("flush", "ログインしました。");
        response.sendRedirect(request.getContextPath() + "/");

    }

}
