package checks;

import java.io.IOException;
import java.sql.SQLException;

class CombineCatchCheckWithVersion {
  void foo() {
    CombineCatchCheckLogger2 logger = new CombineCatchCheckLogger2();

    try {
      canThrow();
    } catch (IOException e) {
//    ^[el=+4;ec=5]>
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) { // Noncompliant {{Combine this catch with the one at line 12, which has the same body.}}
//         ^^^^^^^^^^^^^^
      doCleanup();
      logger.log(e);
    }
    catch (IllegalArgumentException  e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }
   catch (ArrayStoreException e) { // Noncompliant {{Combine this catch with the one at line 17, which has the same body.}}
//        ^^^^^^^^^^^^^^^^^^^^^
    doCleanup();
    logger.log(e);
  }

    try {
      canThrow();
    } catch (IOException | java.lang.IllegalArgumentException e) {
//    ^[el=+4;ec=5]>
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) { // Noncompliant {{Combine this catch with the one at line 34, which has the same body.}}
//         ^^^^^^^^^^^^^^
      doCleanup();
      logger.log(e);
    }
    catch (ArrayStoreException  e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }

    try {
      canThrow();
    } catch (IOException e) {
      handleException(e);
    } catch (IllegalArgumentException e) { // Compliant, specific type is need in the body
      handleException(e);
    } catch (SQLException e) { // Compliant, not the same body
      handleException(e, "a");
    } catch (Exception e) { // Compliant
      int i;
    } catch (Throwable e) { // Compliant
      doCleanup();
    }
  }

  void canThrow() throws IOException, SQLException, IllegalArgumentException {
  }

  private void doCleanup() {
  }

  void handleException(IOException io) { }

  void handleException(IllegalArgumentException io) { }

  void handleException(SQLException io, String s) { }
}

class CombineCatchCheckLogger2 {
  void log(Exception e) {
  }
}
