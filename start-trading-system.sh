#!/bin/bash

# 🚀 Quantitative Trading System - Complete Launcher
# This script starts both backend and frontend components

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BACKEND_PORT=8080
BACKEND_HEALTH_URL="http://localhost:$BACKEND_PORT/actuator/health"
BACKEND_MAX_WAIT=120  # Maximum wait time for backend startup (seconds)
FRONTEND_DELAY=5      # Additional delay before starting frontend

# PID files for process management
BACKEND_PID_FILE="/tmp/trading-backend.pid"
FRONTEND_PID_FILE="/tmp/trading-frontend.pid"

# Log files
LOG_DIR="./logs"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

# Create logs directory
mkdir -p "$LOG_DIR"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[$(date '+%H:%M:%S')] ✅ $1${NC}"
}

print_error() {
    echo -e "${RED}[$(date '+%H:%M:%S')] ❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}[$(date '+%H:%M:%S')] ⚠️  $1${NC}"
}

print_info() {
    echo -e "${CYAN}[$(date '+%H:%M:%S')] ℹ️  $1${NC}"
}

# Function to check if a process is running
is_process_running() {
    local pid_file=$1
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0  # Process is running
        else
            rm -f "$pid_file"  # Clean up stale PID file
            return 1  # Process is not running
        fi
    fi
    return 1  # PID file doesn't exist
}

# Function to wait for backend health check
wait_for_backend() {
    print_status "Waiting for backend to be ready..."
    local count=0
    local max_attempts=$((BACKEND_MAX_WAIT / 2))
    
    while [ $count -lt $max_attempts ]; do
        if curl -s -f "$BACKEND_HEALTH_URL" > /dev/null 2>&1; then
            print_success "Backend is ready and healthy!"
            return 0
        fi
        
        count=$((count + 1))
        printf "${YELLOW}⏳ Waiting for backend... (%d/%d)${NC}\r" $count $max_attempts
        sleep 2
    done
    
    echo  # New line after progress
    print_error "Backend failed to start within $BACKEND_MAX_WAIT seconds"
    return 1
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 17 or higher."
        exit 1
    fi
    
    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "Java 17 or higher is required. Current version: $JAVA_VERSION"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven 3.6 or higher."
        exit 1
    fi
    
    # Check if backend directory exists
    if [ ! -d "./src" ] || [ ! -f "./pom.xml" ]; then
        print_error "Backend source code not found. Please run this script from the backend root directory."
        exit 1
    fi
    
    # Check if frontend directory exists
    if [ ! -d "./frontend" ]; then
        print_error "Frontend directory not found. Please ensure the frontend is in './frontend' directory."
        exit 1
    fi
    
    print_success "All prerequisites satisfied"
}

# Function to start backend
start_backend() {
    print_status "Starting backend server..."
    
    # Check if backend is already running
    if is_process_running "$BACKEND_PID_FILE"; then
        print_warning "Backend is already running (PID: $(cat $BACKEND_PID_FILE))"
        return 0
    fi
    
    # Check if port is in use
    if lsof -Pi :$BACKEND_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        print_error "Port $BACKEND_PORT is already in use. Please stop the existing service."
        exit 1
    fi
    
    # Build backend
    print_status "Building backend..."
    if ! mvn clean compile -q > "$BACKEND_LOG" 2>&1; then
        print_error "Backend build failed. Check $BACKEND_LOG for details."
        exit 1
    fi
    print_success "Backend build completed"
    
    # Start backend in background
    print_status "Launching backend server..."
    nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev \
        > "$BACKEND_LOG" 2>&1 &
    
    local backend_pid=$!
    echo $backend_pid > "$BACKEND_PID_FILE"
    
    print_success "Backend started (PID: $backend_pid)"
    print_info "Backend logs: $BACKEND_LOG"
    
    # Wait for backend to be ready
    if ! wait_for_backend; then
        print_error "Backend startup failed"
        stop_backend
        exit 1
    fi
}

# Function to start frontend
start_frontend() {
    print_status "Starting frontend application..."
    
    # Check if frontend is already running
    if is_process_running "$FRONTEND_PID_FILE"; then
        print_warning "Frontend is already running (PID: $(cat $FRONTEND_PID_FILE))"
        return 0
    fi
    
    # Navigate to frontend directory
    cd frontend
    
    # Build frontend
    print_status "Building frontend..."
    if ! mvn clean compile -q > "../$FRONTEND_LOG" 2>&1; then
        print_error "Frontend build failed. Check $FRONTEND_LOG for details."
        cd ..
        exit 1
    fi
    print_success "Frontend build completed"
    
    # Additional delay to ensure backend is fully ready
    print_status "Waiting $FRONTEND_DELAY seconds for backend to stabilize..."
    sleep $FRONTEND_DELAY
    
    # Start frontend
    print_status "Launching frontend application..."
    nohup mvn javafx:run > "../$FRONTEND_LOG" 2>&1 &
    
    local frontend_pid=$!
    echo $frontend_pid > "$FRONTEND_PID_FILE"
    
    print_success "Frontend started (PID: $frontend_pid)"
    print_info "Frontend logs: $FRONTEND_LOG"
    
    # Return to original directory
    cd ..
}

# Function to stop backend
stop_backend() {
    if is_process_running "$BACKEND_PID_FILE"; then
        local pid=$(cat "$BACKEND_PID_FILE")
        print_status "Stopping backend (PID: $pid)..."
        
        # Try graceful shutdown first
        kill -TERM "$pid" 2>/dev/null || true
        
        # Wait for graceful shutdown
        local count=0
        while [ $count -lt 10 ] && ps -p "$pid" > /dev/null 2>&1; do
            sleep 1
            count=$((count + 1))
        done
        
        # Force kill if still running
        if ps -p "$pid" > /dev/null 2>&1; then
            print_warning "Force killing backend process..."
            kill -KILL "$pid" 2>/dev/null || true
        fi
        
        rm -f "$BACKEND_PID_FILE"
        print_success "Backend stopped"
    fi
}

# Function to stop frontend
stop_frontend() {
    if is_process_running "$FRONTEND_PID_FILE"; then
        local pid=$(cat "$FRONTEND_PID_FILE")
        print_status "Stopping frontend (PID: $pid)..."
        
        # Try graceful shutdown first
        kill -TERM "$pid" 2>/dev/null || true
        
        # Wait for graceful shutdown
        local count=0
        while [ $count -lt 5 ] && ps -p "$pid" > /dev/null 2>&1; do
            sleep 1
            count=$((count + 1))
        done
        
        # Force kill if still running
        if ps -p "$pid" > /dev/null 2>&1; then
            kill -KILL "$pid" 2>/dev/null || true
        fi
        
        rm -f "$FRONTEND_PID_FILE"
        print_success "Frontend stopped"
    fi
}

# Function to stop all services
stop_all() {
    print_status "Stopping all services..."
    stop_frontend
    stop_backend
    print_success "All services stopped"
}

# Function to show status
show_status() {
    echo
    print_info "=== Trading System Status ==="
    
    if is_process_running "$BACKEND_PID_FILE"; then
        local backend_pid=$(cat "$BACKEND_PID_FILE")
        print_success "Backend: Running (PID: $backend_pid)"
        
        # Check health endpoint
        if curl -s -f "$BACKEND_HEALTH_URL" > /dev/null 2>&1; then
            print_success "Backend Health: OK"
        else
            print_warning "Backend Health: Not responding"
        fi
    else
        print_error "Backend: Not running"
    fi
    
    if is_process_running "$FRONTEND_PID_FILE"; then
        local frontend_pid=$(cat "$FRONTEND_PID_FILE")
        print_success "Frontend: Running (PID: $frontend_pid)"
    else
        print_error "Frontend: Not running"
    fi
    
    echo
    print_info "Log files:"
    print_info "  Backend:  $BACKEND_LOG"
    print_info "  Frontend: $FRONTEND_LOG"
    echo
}

# Function to show help
show_help() {
    echo
    echo -e "${PURPLE}🚀 Quantitative Trading System Launcher${NC}"
    echo
    echo "Usage: $0 [COMMAND]"
    echo
    echo "Commands:"
    echo "  start     Start both backend and frontend (default)"
    echo "  stop      Stop all services"
    echo "  restart   Restart all services"
    echo "  status    Show service status"
    echo "  backend   Start only backend"
    echo "  frontend  Start only frontend"
    echo "  logs      Show recent logs"
    echo "  help      Show this help message"
    echo
    echo "Examples:"
    echo "  $0                    # Start complete system"
    echo "  $0 start              # Start complete system"
    echo "  $0 stop               # Stop all services"
    echo "  $0 status             # Check status"
    echo "  $0 logs               # View recent logs"
    echo
}

# Function to show logs
show_logs() {
    echo
    print_info "=== Recent Backend Logs ==="
    if [ -f "$BACKEND_LOG" ]; then
        tail -20 "$BACKEND_LOG"
    else
        print_warning "No backend logs found"
    fi
    
    echo
    print_info "=== Recent Frontend Logs ==="
    if [ -f "$FRONTEND_LOG" ]; then
        tail -20 "$FRONTEND_LOG"
    else
        print_warning "No frontend logs found"
    fi
    echo
}

# Trap to handle script interruption
trap 'echo; print_warning "Script interrupted. Cleaning up..."; stop_all; exit 130' INT TERM

# Main execution
main() {
    local command=${1:-start}
    
    case $command in
        "start")
            echo
            echo -e "${PURPLE}🚀 Starting Quantitative Trading System${NC}"
            echo -e "${PURPLE}=======================================${NC}"
            echo
            
            check_prerequisites
            start_backend
            start_frontend
            
            echo
            print_success "🎉 Trading System started successfully!"
            echo
            print_info "Access points:"
            print_info "  Backend API: http://localhost:$BACKEND_PORT"
            print_info "  Health Check: $BACKEND_HEALTH_URL"
            print_info "  Frontend: Desktop application window"
            echo
            print_info "Use '$0 stop' to stop all services"
            print_info "Use '$0 status' to check service status"
            print_info "Use '$0 logs' to view recent logs"
            echo
            ;;
        "stop")
            stop_all
            ;;
        "restart")
            print_status "Restarting Trading System..."
            stop_all
            sleep 2
            $0 start
            ;;
        "status")
            show_status
            ;;
        "backend")
            check_prerequisites
            start_backend
            print_success "Backend started. Use '$0 frontend' to start frontend."
            ;;
        "frontend")
            start_frontend
            print_success "Frontend started."
            ;;
        "logs")
            show_logs
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            print_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"