import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';

import { Button } from '@/components/ui/button';
import { Navbar, NavbarContent, NavbarSection } from '@/components/ui/navbar';
import { useAuth } from '@/hooks/useAuth';
import { cn } from '@/lib/utils';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  cn('text-sm font-medium text-muted-foreground transition-colors hover:text-foreground', isActive && 'text-foreground');

const NavBar = () => {
  const { isAuthenticated, isInitiating, initiateConnect, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleConnect = () => {
    void initiateConnect(location.pathname).catch((error) => {
      console.error('Unable to start Stripe Connect onboarding', error);
    });
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <Navbar className="border-b border-muted bg-background/90 backdrop-blur">
      <NavbarContent className="max-w-6xl">
        <NavbarSection className="gap-6">
          <Link to="/" className="text-base font-semibold tracking-tight text-foreground">
            EnsureBack
          </Link>
          <a
            href="/#process-flow"
            className="hidden text-sm font-medium text-muted-foreground transition-colors hover:text-foreground md:inline"
          >
            How it works
          </a>
          <a
            href="/#faq"
            className="hidden text-sm font-medium text-muted-foreground transition-colors hover:text-foreground md:inline"
          >
            FAQ
          </a>
          <NavLink to="/developer" className={navLinkClass}>
            Docs
          </NavLink>
          <NavLink to="/security" className={navLinkClass}>
            Security
          </NavLink>
          <NavLink to="/status" className={navLinkClass}>
            Status
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/merchant/dashboard" className={navLinkClass} end>
              Merchant
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/dashboard/buyer" className={navLinkClass}>
              Buyer
            </NavLink>
          )}
          {isAuthenticated && (
            <NavLink to="/dashboard/admin" className={navLinkClass}>
              Admin
            </NavLink>
          )}
        </NavbarSection>
        <NavbarSection className="gap-3">
          {!isAuthenticated ? (
            <>
              <Button asChild variant="ghost" size="sm" className="hidden md:inline-flex">
                <a href="mailto:sales@ensureback.com">Book demo</a>
              </Button>
              <Button onClick={handleConnect} disabled={isInitiating} size="sm">
                Get started
              </Button>
            </>
          ) : (
            <Button variant="outline" size="sm" onClick={handleLogout}>
              Log out
            </Button>
          )}
        </NavbarSection>
      </NavbarContent>
    </Navbar>
  );
};

export default NavBar;
