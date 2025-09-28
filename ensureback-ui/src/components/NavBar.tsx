import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import { Navbar, NavbarContent, NavbarSection } from '@/components/ui/navbar';
import { useAuth } from '@/hooks/useAuth';
import { cn } from '@/lib/utils';

const activeLink = ({ isActive }: { isActive: boolean }) =>
  cn(
    'text-sm font-medium text-muted-foreground transition-colors hover:text-foreground',
    isActive && 'text-foreground'
  );

const NavBar = () => {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    if (location.pathname !== '/login') {
      navigate('/login');
    }
  };

  return (
    <Navbar>
      <NavbarContent>
        <NavbarSection className="gap-6">
          <Link to="/" className="text-base font-semibold tracking-tight">
            EnsureBack
          </Link>
          <a href="/#pricing" className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
            Pricing
          </a>
          <a href="/#how-it-works" className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
            How it works
          </a>
          <NavLink to="/developer" className={activeLink}>
            Developer Center
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/dashboard" className={activeLink}>
              Dashboard
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/buyer/orders" className={activeLink}>
              Buyer Portal
            </NavLink>
          )}
        </NavbarSection>
        <NavbarSection className="gap-3">
          {!isAuthenticated ? (
            <>
              <Button asChild variant="ghost">
                <Link to="/login">Login</Link>
              </Button>
              <Button asChild>
                <Link to={isAuthenticated ? '/dashboard' : '/login'}>Get Started</Link>
              </Button>
            </>
          ) : (
            <Button variant="outline" onClick={handleLogout}>
              Log out
            </Button>
          )}
        </NavbarSection>
      </NavbarContent>
    </Navbar>
  );
};

export default NavBar;
