import { ReactNode } from "react";
import Sidebar from "./Sidebar";
import BottomNav from "../BottomNav";

interface AppLayoutProps {
  children: ReactNode;
}

const AppLayout = ({ children }: AppLayoutProps) => {
  return (
    <div className="flex min-h-screen bg-background relative w-full h-screen overflow-hidden">
      <Sidebar />
      <div className="flex-1 md:pl-64 w-full h-full min-h-screen flex flex-col">
        <main className="flex-1 w-full pb-20 md:pb-0 h-full overflow-y-auto overflow-x-hidden relative">
          {children}
        </main>
      </div>
      <div className="md:hidden">
        <BottomNav />
      </div>
    </div>
  );
};

export default AppLayout;
